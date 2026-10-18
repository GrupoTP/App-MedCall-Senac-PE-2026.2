package grupotp.recifesaudavel.common.scheduling

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * @property professionalId null lets the server pick any professional free at that time.
 * @property preTriageId pre-triage to attach to the new appointment, or null to attach none.
 */
@Serializable
data class CreateAppointmentRequest(
    val specialtyId: String,
    val professionalId: String?,
    val date: LocalDate,
    val time: LocalTime,
    val reason: String,
    val notes: String,
    val preTriageId: String? = null,
)
