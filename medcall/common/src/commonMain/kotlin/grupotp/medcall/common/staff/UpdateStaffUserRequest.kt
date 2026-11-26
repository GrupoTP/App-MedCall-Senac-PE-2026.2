package grupotp.medcall.common.staff

import kotlinx.serialization.Serializable

/** Null fields stay as they are. */
@Serializable
data class UpdateStaffUserRequest(
    val fullName: String? = null,
    val role: StaffRole? = null,
    val active: Boolean? = null,
)

