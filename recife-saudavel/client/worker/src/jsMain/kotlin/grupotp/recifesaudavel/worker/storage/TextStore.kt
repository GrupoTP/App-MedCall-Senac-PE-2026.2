package grupotp.recifesaudavel.worker.storage

import kotlinx.coroutines.suspendCancellableCoroutine
import web.events.EventHandler
import web.idb.IDBKeyRange
import web.idb.IDBObjectStore
import web.idb.IDBRequest
import web.idb.IDBValidKey
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * An IndexedDB object store of text values under string keys, valid only inside the transaction
 * that opened it. The transaction commits as soon as no request is pending, so every method queues
 * all of its requests before suspending.
 */
class TextStore(private val store: IDBObjectStore) {
    suspend fun get(key: String): String? =
        store.get(IDBValidKey(key)).await()?.toString()?.takeIf { it.isNotBlank() }

    suspend fun put(key: String, value: String) {
        store.put(value.toJsString(), IDBValidKey(key)).await()
    }

    suspend fun clear() {
        store.clear().await()
    }

    /** Leaves [value] under [key] as the only entry of the store. */
    suspend fun replaceAll(key: String, value: String) {
        store.clear()
        put(key, value)
    }

    /** Deletes every entry except the one under [key]. */
    suspend fun deleteAllExcept(key: String) {
        store.delete(IDBKeyRange.upperBound(key.toJsString(), true))
        store.delete(IDBKeyRange.lowerBound(key.toJsString(), true)).await()
    }

    private suspend fun <T : JsAny?> IDBRequest<T>.await(): T = suspendCancellableCoroutine { continuation ->
        onsuccess = EventHandler { continuation.resume(result) }
        onerror = EventHandler {
            continuation.resumeWithException(IllegalStateException("Local database request failed: ${error?.message}"))
        }
    }
}
