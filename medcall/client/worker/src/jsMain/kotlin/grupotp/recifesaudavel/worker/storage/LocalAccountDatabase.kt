package grupotp.recifesaudavel.worker.storage

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.serialization.json.Json

/**
 * What the device keeps of the signed-in account: its registration profile, its appointments and
 * the sign-outs that still have to reach the server. Values are stored as JSON text.
 * The password is not part of [AuthenticatedAccount] and is never written here.
 */
class LocalAccountDatabase(private val json: Json) {
    private companion object {
        const val NAME = "recife-saudavel"

        /** Version 2 added the appointments store. */
        const val VERSION = 2
        const val ACCOUNT_STORE = "accounts"
        const val APPOINTMENT_STORE = "appointments"
        val STORES = listOf(ACCOUNT_STORE, APPOINTMENT_STORE)

        const val SIGNED_IN_KEY = "signed-in"
        const val PENDING_SIGN_OUTS_KEY = "pending-sign-outs"
    }

    private val database = IndexedDatabase(NAME, VERSION, STORES)

    suspend fun saveAccount(account: AuthenticatedAccount) {
        database.write(ACCOUNT_STORE) { it.put(SIGNED_IN_KEY, json.encodeToString(account)) }
    }

    suspend fun loadAccount(): AuthenticatedAccount? =
        database.read(ACCOUNT_STORE) { it.get(SIGNED_IN_KEY) }?.let { json.decodeFromString(it) }

    /** Keeps only the latest list of one account, so another person on the device never sees it. */
    suspend fun saveAppointments(accountId: String, appointments: List<Appointment>) {
        database.write(APPOINTMENT_STORE) { it.replaceAll(accountId, json.encodeToString(appointments)) }
    }

    suspend fun loadAppointments(accountId: String): List<Appointment>? =
        database.read(APPOINTMENT_STORE) { it.get(accountId) }?.let { json.decodeFromString(it) }

    /** Kept in the accounts store but outside [clear], so signing out does not lose them. */
    suspend fun savePendingSignOuts(tokens: List<SecretString>) {
        database.write(ACCOUNT_STORE) { it.put(PENDING_SIGN_OUTS_KEY, json.encodeToString(tokens)) }
    }

    suspend fun loadPendingSignOuts(): List<SecretString>? =
        database.read(ACCOUNT_STORE) { it.get(PENDING_SIGN_OUTS_KEY) }?.let { json.decodeFromString(it) }

    /**
     * Wipes everything the device keeps, except the sign-outs that still have to reach the server.
     * Every other key and store goes too, so data added later is not left behind.
     */
    suspend fun clear() {
        database.write(ACCOUNT_STORE) { it.deleteAllExcept(PENDING_SIGN_OUTS_KEY) }
        for (store in STORES - ACCOUNT_STORE) {
            database.write(store) { it.clear() }
        }
    }
}
