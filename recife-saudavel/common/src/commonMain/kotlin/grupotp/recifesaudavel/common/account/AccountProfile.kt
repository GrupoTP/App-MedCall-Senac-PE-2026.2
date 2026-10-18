package grupotp.recifesaudavel.common.account

import kotlinx.serialization.Serializable

/**
 * Everything the settings screen shows about the signed-in person.
 * Phones are digits only. Dates are epoch milliseconds, null when the server never recorded them
 * (accounts created before the server kept them).
 */
@Serializable
data class AccountProfile(
    val fullName: String,
    val socialName: String,
    val email: String,
    val primaryPhone: String,
    val primaryPhoneWhatsapp: Boolean,
    val secondaryPhone: String,
    val secondaryPhoneWhatsapp: Boolean,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val acceptedPrivacyPolicy: Boolean,
    val acceptedTreatment: Boolean,
    val wantsNotifications: Boolean,
    val createdAt: Long? = null,
    val lastLoginAt: Long? = null,
    val deletionRequestedAt: Long? = null,
)
