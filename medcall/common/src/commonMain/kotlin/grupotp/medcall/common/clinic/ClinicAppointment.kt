package grupotp.medcall.common.clinic

import grupotp.medcall.common.account.Phone
import grupotp.medcall.common.scheduling.AppointmentStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * One appointment of the clinic's agenda, as the reception sees it.
 *
 * @property date clinic date; [time] clinic wall-clock time.
 * @property pendingSince when it is still pending, when the patient booked it.
 * @property attendanceUndoUntil while the attendance mark can still be undone.
 */
@Serializable
data class ClinicAppointment(
    val id: Int,
    val date: LocalDate,
    val time: LocalTime,
    val status: AppointmentStatus,
    val patientName: String,
    val patientPhone: Phone,
    val specialtyId: Int,
    val specialtyName: String,
    val professionalId: Int,
    val professionalName: String,
    val reason: String = "",
    val notes: String = "",
    val createdAt: Instant,
    val rescheduled: Boolean = false,
    val declineReason: String = "",
    val hasPreTriage: Boolean = false,
    val attendanceUndoUntil: Instant? = null,
)

