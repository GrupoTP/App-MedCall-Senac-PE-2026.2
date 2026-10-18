package grupotp.recifesaudavel.common.account

import kotlinx.serialization.Serializable

/**
 * @property signOutOtherSessions also end every other session of the account, on any device.
 * The session that sends the request stays.
 */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val signOutOtherSessions: Boolean = false,
)
