package grupotp.recifesaudavel.common.scheduling

import grupotp.recifesaudavel.common.phone.Phone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * Appointment of the signed-in patient. Names are copied so the list reads well offline.
 *
 * @property date date at the [clinic], in its time zone.
 * @property time wall-clock time at the [clinic].
 * @property preTriageId pre-triage attached when booking, if any.
 */
@Serializable
data class Appointment(
    val id: String,
    val specialtyName: String,
    val professionalName: String,
    val clinic: Clinic,
    val date: LocalDate,
    val time: LocalTime,
    val status: AppointmentStatus,
    val patientName: String,
    val contactPhone: Phone,
    val reason: String = "",
    val notes: String = "",
    val preTriageId: String? = null,
) {
    /** Start in the clinic's wall clock. To compare with now, convert now to [Clinic.timeZone]. */
    val startsAt: LocalDateTime get() = LocalDateTime(date, time)

    /**
     * Whether the patient may still cancel. A pending appointment can be cancelled even after its
     * time, since the clinic may never have confirmed it; a confirmed one only before it starts.
     *
     * @param nowAtClinic the current moment in the [clinic]'s time zone, not the device's or the server's.
     */
    fun isCancellableAt(nowAtClinic: LocalDateTime): Boolean = when (status) {
        AppointmentStatus.Pending -> true
        AppointmentStatus.Confirmed -> startsAt > nowAtClinic
        AppointmentStatus.Cancelled -> false
    }
}
