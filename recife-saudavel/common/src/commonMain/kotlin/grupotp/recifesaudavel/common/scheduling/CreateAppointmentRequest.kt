package grupotp.recifesaudavel.common.scheduling

import kotlinx.serialization.Serializable

/**
 * @property professionalId null lets the server pick any professional free at that time.
 * @property preTriageId pre-triage to attach to the new appointment, or null to attach none.
 */
@Serializable
data class CreateAppointmentRequest(
    val specialtyId: String,
    val professionalId: String?,
    val date: String,
    val time: String,
    val reason: String,
    val notes: String,
    val preTriageId: String? = null,
)
