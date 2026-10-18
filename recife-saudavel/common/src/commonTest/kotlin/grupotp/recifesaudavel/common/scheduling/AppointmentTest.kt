package grupotp.recifesaudavel.common.scheduling

import grupotp.recifesaudavel.common.phone.Phone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppointmentTest {
    private val appointment = Appointment(
        id = "a1",
        specialtyName = "Cardiologia",
        professionalName = "Dra. Lúcia Oliveira",
        date = LocalDate(2026, 10, 14),
        time = LocalTime(9, 30),
        status = AppointmentStatus.Confirmed,
        patientName = "Maria",
        contactPhone = Phone("81999990000"),
    )

    @Test
    fun datesAndTimesTravelAsIsoText() {
        // Act
        val json = Json.encodeToString(AvailableDay(LocalDate(2026, 10, 14), listOf(LocalTime(8, 0), LocalTime(14, 30))))

        // Assert
        assertEquals("""{"date":"2026-10-14","times":["08:00","14:30"]}""", json)
    }

    @Test
    fun aConfirmedAppointmentIsCancellableOnlyBeforeItStarts() {
        // Assert
        assertTrue(appointment.isCancellableAt(LocalDateTime(2026, 10, 14, 9, 29)))
        assertFalse(appointment.isCancellableAt(LocalDateTime(2026, 10, 14, 9, 30)))
        assertTrue(appointment.copy(status = AppointmentStatus.Pending).isCancellableAt(LocalDateTime(2026, 12, 1, 0, 0)))
        assertFalse(appointment.copy(status = AppointmentStatus.Cancelled).isCancellableAt(LocalDateTime(2026, 1, 1, 0, 0)))
    }
}
