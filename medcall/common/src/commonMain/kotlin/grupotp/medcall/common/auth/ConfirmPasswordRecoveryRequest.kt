package grupotp.medcall.common.auth

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.Serializable

/** Uses a recovery code once and replaces the password. */
@Serializable
data class ConfirmPasswordRecoveryRequest(
    val email: String,
    val code: SecretString,
    val newPassword: SecretString,
)
