package grupotp.recifesaudavel.server.database

import org.jetbrains.exposed.v1.core.Table

/**
 * Bearer sessions. Only the SHA-256 of the token is stored, so a database copy cannot sign in.
 */
object SessionsTable : Table("sessions") {
    val tokenHash = varchar("token_hash", 64)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(tokenHash)
}
