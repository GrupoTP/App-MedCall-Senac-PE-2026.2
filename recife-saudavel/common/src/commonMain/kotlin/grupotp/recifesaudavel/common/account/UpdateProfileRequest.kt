package grupotp.recifesaudavel.common.account

import kotlinx.serialization.Serializable

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
