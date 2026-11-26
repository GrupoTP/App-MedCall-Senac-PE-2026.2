package grupotp.medcall.common.integration

import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.scheduling.AppointmentStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** An appointment as the public API and the webhooks show it to the clinic's own system. */
@Serializable
data class PublicAppointment(
    val id: Int,
    val status: AppointmentStatus,
    val date: LocalDate,
    val time: LocalTime,
    val professionalId: Int,
    val professionalName: String,
    val specialtyName: String,
    val patientName: String,
    val patientPhone: String,
    val reason: String = "",
    val createdAt: Instant,
    val lastEvent: AppointmentEventType? = null,
)

