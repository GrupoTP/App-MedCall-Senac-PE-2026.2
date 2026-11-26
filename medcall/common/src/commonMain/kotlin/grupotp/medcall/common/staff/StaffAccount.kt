package grupotp.medcall.common.staff

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.Serializable

/**
 * The signed-in staff member.
 *
 * @property clinicId null for [StaffRole.MedcallAdmin].
 * @property mustChangePassword true while the account still has its temporary password; every
 * other staff route answers 403 `PASSWORD_CHANGE_REQUIRED` until it is changed.
 * @property sessionToken only filled in the sign-in answer.
 */
@Serializable
data class StaffAccount(
    val id: Int,
    val fullName: String,
    val email: String,
    val role: StaffRole,
    val clinicId: Int? = null,
    val clinicName: String? = null,
    val mustChangePassword: Boolean = false,
    val sessionToken: SecretString = SecretString.EMPTY,
)

