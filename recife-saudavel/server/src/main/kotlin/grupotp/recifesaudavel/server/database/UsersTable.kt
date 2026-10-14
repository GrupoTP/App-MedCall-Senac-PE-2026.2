package grupotp.recifesaudavel.server.database

import org.jetbrains.exposed.v1.core.Table

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val fullName = varchar("full_name", 200)
    val socialName = varchar("social_name", 200)
    val email = varchar("email", 200)
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

    // Nullable: accounts created before these columns existed have no value.
    val createdAt = long("created_at").nullable()
    val lastLoginAt = long("last_login_at").nullable()
    val deletionRequestedAt = long("deletion_requested_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(email)
    }
}
