@file:JsModule("expo-sqlite")

package grupotp.recifesaudavel.app.gateway.sqlite

import js.promise.Promise

/**
 * Subset of the expo-sqlite async API. Metro replaces the module with an empty one on the web,
 * so nothing here may run unless the platform is native.
 */
internal external interface SQLiteDatabase {
    fun execAsync(source: String): Promise<Nothing?>

    fun runAsync(source: String, params: Array<out Any?>): Promise<SQLiteRunResult>

    fun <T> getFirstAsync(source: String, params: Array<out Any?>): Promise<T?>

    fun closeAsync(): Promise<Nothing?>
}

internal external interface SQLiteRunResult {
    val lastInsertRowId: Double
    val changes: Double
}

internal external fun openDatabaseAsync(databaseName: String): Promise<SQLiteDatabase>
