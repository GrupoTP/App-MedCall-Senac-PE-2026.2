package grupotp.recifesaudavel.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object UsersTable : IntIdTable("users") {
    val fullName = varchar("full_name", 200)
    val socialName = varchar("social_name", 200)
    val email = varchar("email", 200).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val primaryPhone = varchar("primary_phone", 40)
    val primaryPhoneWhatsapp = bool("primary_phone_whatsapp")
    val secondaryPhone = varchar("secondary_phone", 40)
    val secondaryPhoneWhatsapp = bool("secondary_phone_whatsapp")
    val emergencyContactName = varchar("emergency_contact_name", 200)
    val emergencyContactPhone = varchar("emergency_contact_phone", 40)
    val acceptedPrivacyPolicy = bool("accepted_privacy_policy")
    val acceptedTreatment = bool("accepted_treatment")
    val wantsNotifications = bool("wants_notifications")
    val createdAt = timestamp("created_at")
    val lastLoginAt = timestamp("last_login_at")
    val deletionRequestedAt = timestamp("deletion_requested_at").nullable()
}
