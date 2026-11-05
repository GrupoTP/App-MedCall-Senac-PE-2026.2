package grupotp.recifesaudavel.app.platform

/**
 * Loading this module registers the IANA time zones with js-joda, which kotlinx-datetime uses on
 * JavaScript. Without it only UTC and the device zone exist, and `TimeZone.of("America/Manaus")`
 * throws. Each clinic carries its own zone, so the database must be in place before any appointment
 * is read.
 *
 * The 10-year-range build keeps the rules of the years around its release (41 KB instead of 730 KB);
 * later dates follow the last known rule of each zone.
 */
@JsModule("@js-joda/timezone/dist/js-joda-timezone-10-year-range")
@JsNonModule
private external object JsJodaTimeZoneModule

/** Registers the time zone database. Safe to call more than once. */
internal fun loadTimeZones() {
    // Referencing the module makes the bundler keep it and runs its registration.
    JsJodaTimeZoneModule
}
