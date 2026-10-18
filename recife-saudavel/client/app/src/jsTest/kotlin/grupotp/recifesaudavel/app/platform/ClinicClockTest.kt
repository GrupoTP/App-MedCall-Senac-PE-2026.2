package grupotp.recifesaudavel.app.platform

import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class ClinicClockTest {
    @Test
    fun clinicTimeIsRecifeTimeWhateverTheDeviceZone() {
        // Arrange: Recife has been UTC-3 all year since 2020; the device zone does not matter.
        val recife = UtcOffset(hours = -3).asTimeZone()

        // Act
        val before = Clock.System.now().toLocalDateTime(recife)
        val now = clinicNow()
        val after = (Clock.System.now() + 1.minutes).toLocalDateTime(recife)

        // Assert
        assertTrue(now >= before && now <= after, "clinicNow() = $now, expected between $before and $after")
    }
}
