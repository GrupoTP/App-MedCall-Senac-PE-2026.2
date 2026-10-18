package grupotp.recifesaudavel.common.scheduling

import kotlinx.serialization.Serializable

/**
 * Appointment of the signed-in patient. Names are copied so the list reads well offline.
 *
 * @property date ISO date, such as `2026-10-14`.
 * @property time `HH:mm`.
 * @property preTriageId pre-triage attached when booking, if any.
 */
@Serializable
data class Appointment(
    val id: String,
    val specialtyName: String,
    val professionalName: String,
    val date: String,
    val time: String,
    val status: AppointmentStatus,
    val patientName: String,
    val contactPhone: String,
    val reason: String = "",
    val notes: String = "",
    val preTriageId: String? = null,
) {
    /**
     * Whether the patient may still cancel. A pending appointment can be cancelled even after its
     * time, since the clinic may never have confirmed it; a confirmed one only before it starts.
     *
     * @param now date and time in Recife as `yyyy-MM-ddTHH:mm`, which compares as text with [date] and [time].
     */
    fun isCancellableAt(now: String): Boolean = when (status) {
        AppointmentStatus.Pending -> true
        AppointmentStatus.Confirmed -> "${date}T$time" > now
        AppointmentStatus.Cancelled -> false
    }
}
