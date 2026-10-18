package grupotp.recifesaudavel.common

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

@Serializable
data class UpdateProfileRequest(
    val fullName: String,
    val socialName: String,
    val email: String,
    val primaryPhone: String,
    val primaryPhoneWhatsapp: Boolean,
    val secondaryPhone: String,
    val secondaryPhoneWhatsapp: Boolean,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
)

/** Only the optional consent can be changed after registration. */
@Serializable
data class UpdateConsentsRequest(
    val wantsNotifications: Boolean,
)

/**
 * @property signOutOtherSessions also end every other session of the account, on any device.
 * The session that sends the request stays.
 */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val signOutOtherSessions: Boolean = false,
)
