package grupotp.recifesaudavel.server.database

import org.jetbrains.exposed.v1.core.Table

/**
 * Open sessions. The client's token is a JWT signed with `JWT_SECRET` that names one of these
 * rows by [id]; the token itself is never stored, and the id alone cannot sign in.
 */
object SessionsTable : Table("sessions") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val createdAt = long("created_at")
    val lastUsedAt = long("last_used_at")

    override val primaryKey = PrimaryKey(id)
}
