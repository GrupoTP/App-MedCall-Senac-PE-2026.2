package grupotp.recifesaudavel.common.scheduling

import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

/**
 * Where a professional attends, in person or by teleconsultation. Appointment dates and times are
 * wall-clock times in [timeZoneId], whatever the zone of the server or of the patient's device.
 *
 * @property city shown next to times, so the patient knows whose clock they are.
 * @property timeZoneId IANA id, such as `America/Recife` or `America/Manaus`.
 */
@Serializable
data class Clinic(
    val id: String,
    val name: String,
    val city: String,
    val timeZoneId: String,
) {
    /** On JavaScript, named zones need the @js-joda/timezone database loaded (see client/app). */
    val timeZone: TimeZone get() = TimeZone.of(timeZoneId)
}
