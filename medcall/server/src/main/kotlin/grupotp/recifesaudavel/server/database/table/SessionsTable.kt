package grupotp.recifesaudavel.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Open sessions. The client's token is a JWT signed with `JWT_SECRET` that names one of these
 * rows by its id and [createdAt]; the token itself is never stored, and the id alone cannot sign in.
 */
object SessionsTable : IntIdTable("sessions") {
    val user = reference("user_id", UsersTable).index()
    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at")
}
