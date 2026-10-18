package grupotp.recifesaudavel.server.domain

import grupotp.recifesaudavel.common.account.Phone

data class UserProfile(
    val fullName: String,
    val socialName: String,
    val primaryPhone: Phone,
    val primaryPhoneWhatsapp: Boolean,
    val secondaryPhone: Phone,
    val secondaryPhoneWhatsapp: Boolean,
    val emergencyContactName: String,
    val emergencyContactPhone: Phone,
)
