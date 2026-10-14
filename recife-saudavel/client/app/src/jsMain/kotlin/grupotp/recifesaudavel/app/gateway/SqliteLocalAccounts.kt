package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.gateway.sqlite.SQLiteDatabase
import grupotp.recifesaudavel.app.gateway.sqlite.openDatabaseAsync
import grupotp.recifesaudavel.app.usecase.AppShellGateway
import grupotp.recifesaudavel.app.usecase.LocalAccountGateway
import grupotp.recifesaudavel.app.usecase.LocalAppointmentGateway
import grupotp.recifesaudavel.app.usecase.PendingSignOutGateway
import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AuthenticatedAccount
import js.promise.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private const val DATABASE_NAME = "recife-saudavel.db"
private const val SIGNED_IN_KEY = "signed-in"

private const val CREATE_ACCOUNTS =
    "CREATE TABLE IF NOT EXISTS accounts (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)"
private const val SAVE_ACCOUNT = "INSERT OR REPLACE INTO accounts (key, value) VALUES (?, ?)"
private const val LOAD_ACCOUNT = "SELECT value FROM accounts WHERE key = ?"
private const val CLEAR_ACCOUNTS = "DELETE FROM accounts WHERE key = ?"
private const val PENDING_SIGN_OUTS_KEY = "pending-sign-outs"

private const val CREATE_APPOINTMENTS =
    "CREATE TABLE IF NOT EXISTS appointments (account_id TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)"
private const val SAVE_APPOINTMENTS = "INSERT INTO appointments (account_id, value) VALUES (?, ?)"
private const val LOAD_APPOINTMENTS = "SELECT value FROM appointments WHERE account_id = ?"
private const val CLEAR_APPOINTMENTS = "DELETE FROM appointments"

private external interface StoredValue {
    val value: String
}

/**
 * SQLite copy of the signed-in registration profile and its appointments on Android.
 * Same keys and JSON as the web worker database. The password is not part of [AuthenticatedAccount].
 * The native app ships its interface in the APK, so there is no offline shell to prepare beyond the database.
 */
class SqliteLocalAccounts(
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AppShellGateway, LocalAccountGateway, LocalAppointmentGateway, PendingSignOutGateway {
    private val opening = Mutex()
    private var database: SQLiteDatabase? = null

    override suspend fun prepare() {
        open()
    }

    override suspend fun save(account: AuthenticatedAccount) {
        open().runAsync(SAVE_ACCOUNT, arrayOf(SIGNED_IN_KEY, json.encodeToString(account))).await()
    }

    override suspend fun load(): AuthenticatedAccount? {
        val stored = open().getFirstAsync<StoredValue>(LOAD_ACCOUNT, arrayOf(SIGNED_IN_KEY)).await()
            ?: return null
        return json.decodeFromString(stored.value)
    }

    override suspend fun clear() {
        val opened = open()
        opened.execAsync(CLEAR_APPOINTMENTS).await()
        opened.runAsync(CLEAR_ACCOUNTS, arrayOf(SIGNED_IN_KEY)).await()
    }

    override suspend fun loadPendingSignOuts(): List<String> {
        val stored = open().getFirstAsync<StoredValue>(LOAD_ACCOUNT, arrayOf(PENDING_SIGN_OUTS_KEY)).await()
            ?: return emptyList()
        return json.decodeFromString(stored.value)
    }

    override suspend fun savePendingSignOuts(tokens: List<String>) {
        open().runAsync(SAVE_ACCOUNT, arrayOf(PENDING_SIGN_OUTS_KEY, json.encodeToString(tokens))).await()
    }

    /** Keeps only the latest list of one account, so another person on the device never sees it. */
    override suspend fun saveAppointments(accountId: String, appointments: List<Appointment>) {
        val opened = open()
        opened.execAsync(CLEAR_APPOINTMENTS).await()
        opened.runAsync(SAVE_APPOINTMENTS, arrayOf(accountId, json.encodeToString(appointments))).await()
    }

    override suspend fun loadAppointments(accountId: String): List<Appointment>? {
        val stored = open().getFirstAsync<StoredValue>(LOAD_APPOINTMENTS, arrayOf(accountId)).await()
            ?: return null
        return json.decodeFromString(stored.value)
    }

    private suspend fun open(): SQLiteDatabase = opening.withLock {
        database ?: openDatabaseAsync(DATABASE_NAME).await().also { opened ->
            opened.execAsync("$CREATE_ACCOUNTS; $CREATE_APPOINTMENTS").await()
            database = opened
        }
    }
}
