package grupotp.medcall.common.staff

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.Serializable

@Serializable
data class StaffSignInRequest(
    val email: String,
    val password: SecretString,
)

