package grupotp.medcall.common.auth

import kotlinx.serialization.Serializable

/** Asks for a one-time code. The server answers the same way when the e-mail is unknown. */
@Serializable
data class PasswordRecoveryRequest(
    val email: String,
)
