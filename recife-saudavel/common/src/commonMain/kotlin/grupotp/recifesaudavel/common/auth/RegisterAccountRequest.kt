package grupotp.recifesaudavel.common.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterAccountRequest(
    val fullName: String,
    val socialName: String,
    val email: String,
    val password: String,
    val primaryPhone: String,
    val primaryPhoneWhatsapp: Boolean,
    val secondaryPhone: String,
    val secondaryPhoneWhatsapp: Boolean,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val acceptedPrivacyPolicy: Boolean,
    val acceptedTreatment: Boolean,
    val wantsNotifications: Boolean,
)
