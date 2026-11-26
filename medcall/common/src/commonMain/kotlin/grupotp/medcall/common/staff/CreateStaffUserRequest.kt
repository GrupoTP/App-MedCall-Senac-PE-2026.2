package grupotp.medcall.common.staff

import kotlinx.serialization.Serializable

@Serializable
data class CreateStaffUserRequest(
    val fullName: String,
    val email: String,
    val role: StaffRole,
)

