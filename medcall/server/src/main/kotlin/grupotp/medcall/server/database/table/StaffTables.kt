package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Accounts of the clinic portal and of the MedCall panel, kept apart from the patients'
 * [UsersTable]. [clinic] is null only for MedCall's own team.
 *
 * @property role wire name of [grupotp.medcall.common.staff.StaffRole].
 */
object StaffUsersTable : IntIdTable("staff_users") {
    val fullName = varchar("full_name", 200)
    val email = varchar("email", 200).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20)
    val clinic = optReference("clinic_id", ClinicsTable).index()
    val active = bool("active")
    val mustChangePassword = bool("must_change_password")
    val createdAt = timestamp("created_at")
    val lastLoginAt = timestamp("last_login_at").nullable()
}

/** Open staff sessions, like [SessionsTable] for patients. */
object StaffSessionsTable : IntIdTable("staff_sessions") {
    val staffUser = reference("staff_user_id", StaffUsersTable, onDelete = ReferenceOption.CASCADE).index()
    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at")
}

/** Who did what in the portals. Holds names of things, never patient data. */
object AuditLogTable : IntIdTable("audit_log") {
    val occurredAt = timestamp("occurred_at")
    val staffUser = optReference("staff_user_id", StaffUsersTable)
    val apiKey = optReference("api_key_id", ApiKeysTable)
    val clinic = optReference("clinic_id", ClinicsTable).index()
    val action = varchar("action", 60)
    val target = varchar("target", 200)
}
