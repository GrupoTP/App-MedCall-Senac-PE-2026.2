package grupotp.recifesaudavel.common.account

import grupotp.recifesaudavel.common.phone.Phone
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Everything the settings screen shows about the signed-in person.
 * The instants are null when the server never recorded them (accounts created before it kept them).
 */
@Serializable
data class AccountProfile(
    val fullName: String,
    val socialName: String,
    val email: String,
    val primaryPhone: Phone,
    val primaryPhoneWhatsapp: Boolean,
    val secondaryPhone: Phone,
    val secondaryPhoneWhatsapp: Boolean,
    val emergencyContactName: String,
    val emergencyContactPhone: Phone,
    val acceptedPrivacyPolicy: Boolean,
    val acceptedTreatment: Boolean,
    val wantsNotifications: Boolean,
    val createdAt: Instant? = null,
    val lastLoginAt: Instant? = null,
    val deletionRequestedAt: Instant? = null,
)
