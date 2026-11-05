package grupotp.medcall.common.account

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.Serializable

/**
 * @property signOutOtherSessions also end every other session of the account, on any device.
 * The session that sends the request stays.
 */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: SecretString,
    val newPassword: SecretString,
    val signOutOtherSessions: Boolean = false,
)
