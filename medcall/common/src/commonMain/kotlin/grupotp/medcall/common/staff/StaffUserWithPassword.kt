package grupotp.medcall.common.staff

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.Serializable

/**
 * A new account or a password reset. [temporaryPassword] is shown once and never stored in clear.
 */
@Serializable
data class StaffUserWithPassword(
    val user: StaffUser,
    val temporaryPassword: SecretString,
)

