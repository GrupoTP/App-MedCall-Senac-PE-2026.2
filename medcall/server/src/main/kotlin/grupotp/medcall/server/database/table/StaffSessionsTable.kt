package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** Open staff sessions, like [SessionsTable] for patients. */
object StaffSessionsTable : IntIdTable("staff_sessions") {
    val staffUser = reference("staff_user_id", StaffUsersTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at")
}

