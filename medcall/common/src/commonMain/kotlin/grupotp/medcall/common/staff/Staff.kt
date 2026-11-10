package grupotp.medcall.common.staff

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Who a staff account is. Staff accounts are separate from patient accounts: a patient token is
 * refused on the staff routes and the other way around.
 */
@Serializable
enum class StaffRole {
    /** MedCall's own team. Sees the whole platform, only as aggregates. */
    @SerialName("medcall_admin")
    MedcallAdmin,

    /** Manages one clinic: users, dashboard, integrations and the agenda. */
    @SerialName("clinic_admin")
    ClinicAdmin,

    /** Works one clinic's agenda. */
    @SerialName("receptionist")
    Receptionist,
    ;

    val isClinicRole: Boolean get() = this != MedcallAdmin
}

@Serializable
data class StaffSignInRequest(
    val email: String,
    val password: SecretString,
)

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

@Serializable
data class ChangeStaffPasswordRequest(
    val currentPassword: SecretString,
    val newPassword: SecretString,
)

/** A staff account as listed to the people who manage it. */
@Serializable
data class StaffUser(
    val id: Int,
    val fullName: String,
    val email: String,
    val role: StaffRole,
    val clinicId: Int? = null,
    val active: Boolean,
    val mustChangePassword: Boolean,
    val createdAt: Instant,
    val lastLoginAt: Instant? = null,
)

@Serializable
data class CreateStaffUserRequest(
    val fullName: String,
    val email: String,
    val role: StaffRole,
)

/** Null fields stay as they are. */
@Serializable
data class UpdateStaffUserRequest(
    val fullName: String? = null,
    val role: StaffRole? = null,
    val active: Boolean? = null,
)

/**
 * A new account or a password reset. [temporaryPassword] is shown once and never stored in clear.
 */
@Serializable
data class StaffUserWithPassword(
    val user: StaffUser,
    val temporaryPassword: SecretString,
)
