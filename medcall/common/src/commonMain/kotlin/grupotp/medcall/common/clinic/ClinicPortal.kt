package grupotp.medcall.common.clinic

import grupotp.medcall.common.account.Phone
import grupotp.medcall.common.pretriage.PreTriageForm
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.scheduling.Clinic
import grupotp.medcall.common.scheduling.Professional
import grupotp.medcall.common.scheduling.Specialty
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** The signed-in clinic, with what its filters can choose from. */
@Serializable
data class ClinicCatalog(
    val clinic: Clinic,
    val specialties: List<Specialty>,
    val professionals: List<Professional>,
)

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

/** One status change, newest last. */
@Serializable
data class AppointmentHistoryEntry(
    val type: AppointmentEventType,
    val occurredAt: Instant,
    val origin: EventOrigin,
    val actorName: String = "",
    val detail: String = "",
)

@Serializable
data class ClinicAppointmentDetail(
    val appointment: ClinicAppointment,
    val history: List<AppointmentHistoryEntry>,
    val preTriage: PreTriageForm? = null,
)

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

@Serializable
enum class EventOrigin {
    @SerialName("patient_app") PatientApp,
    @SerialName("clinic_portal") ClinicPortal,
    @SerialName("public_api") PublicApi,
    @SerialName("demo_seed") DemoSeed,
}

/** Fixed list, so the dashboard can count them. */
@Serializable
enum class DeclineReason(val label: String) {
    @SerialName("professional_unavailable") ProfessionalUnavailable("Profissional indisponível"),
    @SerialName("clinic_closed") ClinicClosed("Clínica fechada no dia"),
    @SerialName("not_covered") NotCovered("Atendimento não coberto pela clínica"),
    @SerialName("duplicate") Duplicate("Agendamento em duplicidade"),
    @SerialName("other") Other("Outro motivo"),
}

@Serializable
data class DeclineRequest(
    val reason: DeclineReason,
    val note: String = "",
)

@Serializable
data class RescheduleRequest(
    val date: LocalDate,
    val time: LocalTime,
)

@Serializable
data class AttendanceRequest(
    val attended: Boolean,
)

/** Free times of one professional, for a reschedule. */
@Serializable
data class ProfessionalFreeDay(
    val date: LocalDate,
    val times: List<LocalTime>,
)
