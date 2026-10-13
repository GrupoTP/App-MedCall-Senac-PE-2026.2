package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.gateway.sqlite.SQLiteDatabase
import grupotp.recifesaudavel.app.gateway.sqlite.openDatabaseAsync
import grupotp.recifesaudavel.app.usecase.AppShellGateway
import grupotp.recifesaudavel.app.usecase.LocalAccountGateway
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

private external interface StoredAccount {
    val value: String
}

/**
 * SQLite copy of the signed-in registration profile on Android.
 * Same key and JSON as the web worker database. The password is not part of [AuthenticatedAccount].
 * The native app ships its interface in the APK, so there is no offline shell to prepare beyond the database.
 */
class SqliteLocalAccounts(
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AppShellGateway, LocalAccountGateway {
    private val opening = Mutex()
    private var database: SQLiteDatabase? = null

    override suspend fun prepare() {
        open()
    }

    override suspend fun save(account: AuthenticatedAccount) {
        open().runAsync(SAVE_ACCOUNT, arrayOf(SIGNED_IN_KEY, json.encodeToString(account))).await()
    }

    override suspend fun load(): AuthenticatedAccount? {
        val stored = open().getFirstAsync<StoredAccount>(LOAD_ACCOUNT, arrayOf(SIGNED_IN_KEY)).await()
            ?: return null
        return json.decodeFromString(stored.value)
    }

    private suspend fun open(): SQLiteDatabase = opening.withLock {
        database ?: openDatabaseAsync(DATABASE_NAME).await().also { opened ->
            opened.execAsync(CREATE_ACCOUNTS).await()
            database = opened
        }
    }
}
