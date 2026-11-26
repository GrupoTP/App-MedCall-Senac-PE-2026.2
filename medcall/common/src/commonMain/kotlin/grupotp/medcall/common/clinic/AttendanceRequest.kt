package grupotp.medcall.common.clinic

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceRequest(
    val attended: Boolean,
)

