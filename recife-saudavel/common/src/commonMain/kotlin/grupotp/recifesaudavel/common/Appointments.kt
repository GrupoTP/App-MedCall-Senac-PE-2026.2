package grupotp.recifesaudavel.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Specialty(
    val id: String,
    val name: String,
)

@Serializable
data class Professional(
    val id: String,
    val name: String,
    val specialtyId: String,
)

/**
 * Free times of one day, as offered by the server.
 *
 * @property date ISO date, such as `2026-10-14`.
 * @property times `HH:mm` times, in ascending order.
 */
@Serializable
data class AvailableDay(
    val date: String,
    val times: List<String>,
)

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

@Serializable
enum class AppointmentStatus {
    @SerialName("pending")
    Pending,

    @SerialName("confirmed")
    Confirmed,

    @SerialName("cancelled")
    Cancelled,
}

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
)

/**
 * Whether the patient may still cancel. A pending appointment can be cancelled even after its
 * time, since the clinic may never have confirmed it; a confirmed one only before it starts.
 *
 * @param now date and time in Recife as `yyyy-MM-ddTHH:mm`, which compares as text with [Appointment.date] and [Appointment.time].
 */
fun Appointment.isCancellableAt(now: String): Boolean = when (status) {
    AppointmentStatus.Pending -> true
    AppointmentStatus.Confirmed -> "${date}T$time" > now
    AppointmentStatus.Cancelled -> false
}
