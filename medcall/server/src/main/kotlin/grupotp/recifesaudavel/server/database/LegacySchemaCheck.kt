package grupotp.recifesaudavel.server.database

import grupotp.recifesaudavel.server.database.table.UsersTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.vendors.currentDialectMetadata
import java.sql.Types

/**
 * Refuses a database created before ids became numbers and dates became date columns. Exposed
 * creates missing tables but never changes existing columns, so such a database would only fail
 * later, on some query. The data is a mock: delete it and the server creates it again.
 */
object LegacySchemaCheck {
    fun run(database: Database) {
        val idType: Int? = transaction(database) {
            if (!UsersTable.exists()) {
                return@transaction null
            }
            currentDialectMetadata.tableColumns(UsersTable)[UsersTable].orEmpty()
                .firstOrNull { it.name.equals(UsersTable.id.name, ignoreCase = true) }
                ?.jdbcType
        }
        check(idType == null || idType == Types.INTEGER) {
            "The database was created by an older version of the server (text ids). " +
                "Delete it (by default the data folder next to the server) and start again."
        }
    }
}
