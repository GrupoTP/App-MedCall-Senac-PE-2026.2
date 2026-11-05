package grupotp.medcall.common.auth

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val email: String,
    val password: SecretString,
)
