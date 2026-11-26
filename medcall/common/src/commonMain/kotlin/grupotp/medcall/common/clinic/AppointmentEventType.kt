package grupotp.medcall.common.clinic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AppointmentEventType {
    @SerialName("created") Created,
    @SerialName("confirmed") Confirmed,
    @SerialName("declined") Declined,
    @SerialName("rescheduled") Rescheduled,
    @SerialName("cancelled") Cancelled,
    @SerialName("attended") Attended,
    @SerialName("no_show") NoShow,
    @SerialName("attendance_cleared") AttendanceCleared,
}

