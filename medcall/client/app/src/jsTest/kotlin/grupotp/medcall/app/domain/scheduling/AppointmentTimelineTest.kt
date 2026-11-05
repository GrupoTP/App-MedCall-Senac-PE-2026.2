package grupotp.recifesaudavel.app.domain.scheduling

import grupotp.recifesaudavel.app.fake.SampleAppointments
import grupotp.recifesaudavel.app.fake.SampleAppointments.appointment
import grupotp.recifesaudavel.app.fake.SampleAppointments.later
import grupotp.recifesaudavel.app.fake.SampleAppointments.manaus
import grupotp.recifesaudavel.app.fake.SampleAppointments.sooner
import grupotp.recifesaudavel.app.platform.loadTimeZones
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AppointmentTimelineTest {
    @Test
    fun upcomingSkipsPastAndCancelledAppointments() {
        // Arrange
        val past = sooner.copy(id = 90, date = LocalDate(2026, 10, 1))
        val cancelled = later.copy(id = 91, status = AppointmentStatus.Cancelled)

        // Act
        val upcoming = AppointmentTimeline.upcoming(listOf(later, cancelled, past, sooner), now = Instant.parse("2026-10-13T11:00:00Z"))

        // Assert
        assertEquals(listOf(sooner, later), upcoming)
    }

    @Test
    fun appointmentsInOtherZonesArePlacedByTheirOwnClinicClock() {
        // Arrange: 09:00 in Manaus is 13:00 UTC, after 09:30 in Recife (12:30 UTC).
        loadTimeZones()
        val recifeMorning = appointment(id = 3, date = "2026-10-14", time = "09:30", clinic = SampleAppointments.recife)
        val manausMorning = appointment(id = 4, date = "2026-10-14", time = "09:00", clinic = manaus)
        val both = listOf(manausMorning, recifeMorning)

        // Act
        val bothAhead = AppointmentTimeline.upcoming(both, now = Instant.parse("2026-10-14T12:00:00Z"))
        val onlyManaus = AppointmentTimeline.upcoming(both, now = Instant.parse("2026-10-14T12:45:00Z"))

        // Assert
        assertEquals(listOf(recifeMorning, manausMorning), bothAhead)
        assertEquals(listOf(manausMorning), onlyManaus)
    }
}
