package grupotp.recifesaudavel.worker.storage

import kotlinx.coroutines.suspendCancellableCoroutine
import web.events.EventHandler
import web.idb.IDBDatabase
import web.idb.IDBTransactionMode
import web.idb.indexedDB
import web.idb.readonly
import web.idb.readwrite
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * An IndexedDB database made of [TextStore]s. It is opened for each transaction and closed right
 * after, so a newer version of the worker is never blocked by an old connection.
 *
 * @param storeNames created on the first open of each [version]; existing stores keep their data.
 */
class IndexedDatabase(
    private val name: String,
    private val version: Int,
    private val storeNames: List<String>,
) {
    suspend fun <T> read(storeName: String, block: suspend (TextStore) -> T): T =
        transaction(storeName, IDBTransactionMode.readonly, block)

    suspend fun <T> write(storeName: String, block: suspend (TextStore) -> T): T =
        transaction(storeName, IDBTransactionMode.readwrite, block)

    private suspend fun <T> transaction(
        storeName: String,
        mode: IDBTransactionMode,
        block: suspend (TextStore) -> T,
    ): T {
        val database = open()
        try {
            return block(TextStore(database.transaction(storeName, mode).objectStore(storeName)))
        } finally {
            database.close()
        }
    }

    private suspend fun open(): IDBDatabase = suspendCancellableCoroutine { continuation ->
        val request = indexedDB.open(name, version.toDouble())
        request.onupgradeneeded = EventHandler { createMissingStores(request.result) }
        request.onsuccess = EventHandler { continuation.resume(request.result) }
        request.onerror = EventHandler {
            continuation.resumeWithException(IllegalStateException("Could not open the local database $name."))
        }
    }

    private fun createMissingStores(database: IDBDatabase) {
        for (storeName in storeNames) {
            if (!database.objectStoreNames.contains(storeName)) {
                database.createObjectStore(storeName)
            }
        }
    }
}
