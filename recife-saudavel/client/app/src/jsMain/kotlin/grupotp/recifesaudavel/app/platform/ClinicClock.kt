package grupotp.recifesaudavel.app.platform

import grupotp.recifesaudavel.common.scheduling.ClinicTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Loading this module registers the IANA time zones with js-joda, which kotlinx-datetime uses on
 * JavaScript. Without it only UTC and the device zone exist, and `TimeZone.of("America/Recife")` throws.
 *
 * The 10-year-range build keeps the rules of the years around its release (41 KB instead of 730 KB);
 * later dates follow the last known rule, which for Recife has been a fixed UTC-3 since 2020.
 */
@JsModule("@js-joda/timezone/dist/js-joda-timezone-10-year-range")
@JsNonModule
private external object JsJodaTimeZoneModule

/**
 * Date and time at the clinics now, to compare with appointment dates and times. The device may be
 * in another zone (another state, abroad, or a wrong setting): its own wall clock must not be used.
 */
internal fun clinicNow(): LocalDateTime {
    // Referenced here so the bundler keeps the time zone database.
    JsJodaTimeZoneModule
    return Clock.System.now().toLocalDateTime(ClinicTime.zone)
}
