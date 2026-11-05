package grupotp.recifesaudavel.common.auth

import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val email: String,
    val password: SecretString,
)
