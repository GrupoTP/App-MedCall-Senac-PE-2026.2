package grupotp.recifesaudavel.common.scheduling

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * @property professionalId null lets the server pick any professional free at that time.
 * @property clinicId with no professional, the clinic whose times [date] and [time] are. Needed when
 * the specialty is offered in more than one clinic.
 * @property preTriageId pre-triage to attach to the new appointment, or null to attach none.
 */
@Serializable
data class CreateAppointmentRequest(
    val specialtyId: Int,
    val professionalId: Int?,
    val clinicId: Int? = null,
    val date: LocalDate,
    val time: LocalTime,
    val reason: String,
    val notes: String,
    val preTriageId: Int? = null,
)
