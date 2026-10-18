package grupotp.recifesaudavel.common.auth

import grupotp.recifesaudavel.common.phone.Phone
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.serialization.Serializable

@Serializable
data class RegisterAccountRequest(
    val fullName: String,
    val socialName: String,
    val email: String,
    val password: SecretString,
    val primaryPhone: Phone,
    val primaryPhoneWhatsapp: Boolean,
    val secondaryPhone: Phone,
    val secondaryPhoneWhatsapp: Boolean,
    val emergencyContactName: String,
    val emergencyContactPhone: Phone,
    val acceptedPrivacyPolicy: Boolean,
    val acceptedTreatment: Boolean,
    val wantsNotifications: Boolean,
)
