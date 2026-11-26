package grupotp.medcall.common.staff

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.Serializable

@Serializable
data class ChangeStaffPasswordRequest(
    val currentPassword: SecretString,
    val newPassword: SecretString,
)

