package grupotp.recifesaudavel.worker.storage

import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AuthenticatedAccount
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val DATABASE_NAME = "recife-saudavel"

/** Version 2 added the appointments store. */
private const val DATABASE_VERSION = 2
private const val ACCOUNT_STORE = "accounts"
private const val APPOINTMENT_STORE = "appointments"
private const val SIGNED_IN_KEY = "signed-in"

/**
 * IndexedDB of the signed-in registration profile and of its appointments, keyed by account id.
 * The password is not part of [AuthenticatedAccount] and is never written here.
 */
class LocalAccountDatabase(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun save(account: AuthenticatedAccount) {
        withStore(ACCOUNT_STORE, READ_WRITE) { store ->
            await(store.put(json.encodeToString(account), SIGNED_IN_KEY))
        }
    }

    suspend fun load(): AuthenticatedAccount? {
        val stored = withStore(ACCOUNT_STORE, READ_ONLY) { store ->
            await(store.get(SIGNED_IN_KEY))
        }
        val text = stored.asText() ?: return null
        return json.decodeFromString(text)
    }

    /** Forgets the profile and the appointments. */
    suspend fun clear() {
        withStore(ACCOUNT_STORE, READ_WRITE) { store -> await(store.clear()) }
        withStore(APPOINTMENT_STORE, READ_WRITE) { store -> await(store.clear()) }
    }

    /** Keeps only the latest list of one account, so another person on the device never sees it. */
    suspend fun saveAppointments(accountId: String, appointments: List<Appointment>) {
        withStore(APPOINTMENT_STORE, READ_WRITE) { store ->
            // Queue both before suspending: the transaction commits once it has no pending request.
            store.clear()
            await(store.put(json.encodeToString(appointments), accountId))
        }
    }

    suspend fun loadAppointments(accountId: String): List<Appointment>? {
        val stored = withStore(APPOINTMENT_STORE, READ_ONLY) { store ->
            await(store.get(accountId))
        }
        val text = stored.asText() ?: return null
        return json.decodeFromString(text)
    }

    private fun Any?.asText(): String? = this?.toString()?.takeIf { it.isNotBlank() && it != "undefined" }

    private suspend fun <T> withStore(name: String, mode: String, block: suspend (IdbStore) -> T): T {
        val database = open()
        try {
            val store = database.transaction(name, mode).objectStore(name)
            return block(store)
        } finally {
            database.close()
        }
    }

    private suspend fun open(): IdbDatabase = suspendCancellableCoroutine { continuation ->
        val request = indexedDB.open(DATABASE_NAME, DATABASE_VERSION)
        request.onupgradeneeded = {
            val database = request.result
            for (store in listOf(ACCOUNT_STORE, APPOINTMENT_STORE)) {
                if (!database.objectStoreNames.contains(store)) {
                    database.createObjectStore(store)
                }
            }
        }
        request.onsuccess = {
            continuation.resume(request.result)
        }
        request.onerror = {
            continuation.resumeWithException(IllegalStateException("Não foi possível abrir o banco local."))
        }
    }

    private suspend fun await(request: IdbResultRequest): Any? = suspendCancellableCoroutine { continuation ->
        request.onsuccess = {
            continuation.resume(request.result)
        }
        request.onerror = {
            continuation.resumeWithException(IllegalStateException("Não foi possível acessar o banco local."))
        }
    }
}

private const val READ_ONLY = "readonly"
private const val READ_WRITE = "readwrite"

private external val indexedDB: IdbFactory

private external interface IdbFactory {
    fun open(name: String, version: Int): IdbOpenRequest
}

private external interface IdbOpenRequest {
    var onupgradeneeded: () -> Unit
    var onsuccess: () -> Unit
    var onerror: () -> Unit
    val result: IdbDatabase
}

private external interface IdbDatabase {
    val objectStoreNames: IdbNameList
    fun createObjectStore(name: String)
    fun transaction(store: String, mode: String): IdbTransaction
    fun close()
}

private external interface IdbNameList {
    fun contains(name: String): Boolean
}

private external interface IdbTransaction {
    fun objectStore(name: String): IdbStore
}

private external interface IdbStore {
    fun put(value: String, key: String): IdbResultRequest
    fun get(key: String): IdbResultRequest
    fun clear(): IdbResultRequest
}

private external interface IdbResultRequest {
    var onsuccess: () -> Unit
    var onerror: () -> Unit
    val result: Any?
}
