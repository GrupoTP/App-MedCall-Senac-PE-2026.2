package grupotp.recifesaudavel.common.scheduling

import kotlinx.datetime.TimeZone

/**
 * Appointment dates and times are wall-clock times at the clinics, in Recife, whatever the time
 * zone of the server or of the patient's device. Comparing them with "now" must use this zone.
 */
object ClinicTime {
    const val ZONE_ID = "America/Recife"

    /** On JavaScript, named zones need the @js-joda/timezone database loaded (see client/app). */
    val zone: TimeZone get() = TimeZone.of(ZONE_ID)
}
