package grupotp.recifesaudavel.app.platform

import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TimeZonesTest {
    @Test
    fun clinicZonesResolveOnJavaScriptOnceTheDatabaseIsLoaded() {
        // Arrange
        loadTimeZones()
        val moment = Instant.parse("2026-10-20T12:00:00Z")

        // Act
        val recife = TimeZone.of("America/Recife").offsetAt(moment)
        val manaus = TimeZone.of("America/Manaus").offsetAt(moment)

        // Assert
        assertEquals(UtcOffset(hours = -3), recife)
        assertEquals(UtcOffset(hours = -4), manaus)
    }
}
