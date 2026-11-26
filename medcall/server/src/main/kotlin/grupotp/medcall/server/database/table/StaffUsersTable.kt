package grupotp.medcall.server.database.table

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

