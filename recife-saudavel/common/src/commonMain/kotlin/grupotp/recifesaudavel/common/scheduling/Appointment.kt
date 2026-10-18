package grupotp.recifesaudavel.common.scheduling

import grupotp.recifesaudavel.common.phone.Phone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * Appointment of the signed-in patient. Names are copied so the list reads well offline.
 *
 * @property date clinic date, see [ClinicTime].
 * @property time clinic wall-clock time.
 * @property preTriageId pre-triage attached when booking, if any.
 */
@Serializable
data class Appointment(
    val id: String,
    val specialtyName: String,
    val professionalName: String,
    val date: LocalDate,
    val time: LocalTime,
    val status: AppointmentStatus,
    val patientName: String,
    val contactPhone: Phone,
    val reason: String = "",
    val notes: String = "",
    val preTriageId: String? = null,
) {
    val startsAt: LocalDateTime get() = LocalDateTime(date, time)

    /**
     * Whether the patient may still cancel. A pending appointment can be cancelled even after its
     * time, since the clinic may never have confirmed it; a confirmed one only before it starts.
     *
     * @param now current date and time in the clinic zone ([ClinicTime]), not on the device.
     */
    fun isCancellableAt(now: LocalDateTime): Boolean = when (status) {
        AppointmentStatus.Pending -> true
        AppointmentStatus.Confirmed -> startsAt > now
        AppointmentStatus.Cancelled -> false
    }
}
