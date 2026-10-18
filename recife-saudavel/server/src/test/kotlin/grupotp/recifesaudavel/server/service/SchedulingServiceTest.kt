package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.AppointmentStatus
import grupotp.recifesaudavel.common.CreateAppointmentRequest
import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.server.database.AppointmentsTable
import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import grupotp.recifesaudavel.server.database.PreTriagesTable
import grupotp.recifesaudavel.server.database.ProfessionalsTable
import grupotp.recifesaudavel.server.database.SchedulingCatalog
import grupotp.recifesaudavel.server.database.SessionsTable
import grupotp.recifesaudavel.server.database.SpecialtiesTable
import grupotp.recifesaudavel.server.database.UsersTable
import grupotp.recifesaudavel.server.repository.PreTriageRepository
import grupotp.recifesaudavel.server.repository.SchedulingRepository
import grupotp.recifesaudavel.server.repository.UserRepository
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchedulingServiceTest {
    private val recife = ZoneId.of("America/Recife")

    // Monday, 12 October 2026, 10:10 in Recife.
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 12, 10, 10, 0, 0, recife).toInstant(), recife)
    private val cardiology = SchedulingCatalog.specialtyId("Cardiologia")
    private val lucia = SchedulingCatalog.professionalId("Cardiologia", "Dra. Lúcia Oliveira")
    private val antonio = SchedulingCatalog.professionalId("Cardiologia", "Dr. Antônio Ferreira")

    private lateinit var database: Database
    private lateinit var service: SchedulingService
    private lateinit var maria: String
    private lateinit var ana: String

    @BeforeTest
    fun openDatabase() {
        database = DatabaseConnector().connect(
            DatabaseSettings(
                jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = "",
            ),
        )
        transaction(database) {
            SchemaUtils.create(UsersTable, SessionsTable, SpecialtiesTable, ProfessionalsTable, AppointmentsTable, PreTriagesTable)
        }
        val repository = SchedulingRepository(database).also { it.seedCatalogIfEmpty() }
        service = SchedulingService(repository, clock, PreTriageRepository(database))
        val users = UserRepository(database)
        maria = users.insert(UUID.randomUUID().toString(), registration("maria@email.com", "Graça"), "hash")!!.id
        ana = users.insert(UUID.randomUUID().toString(), registration("ana@email.com", ""), "hash")!!.id
    }

    @AfterTest
    fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun availabilityOffersWeekdaysOnlyAndSkipsPastTimesOfToday() {
        // Act
        val days = service.availability(cardiology, professionalId = null)!!

        // Assert
        val today = days.first()
        assertEquals("2026-10-12", today.date)
        assertEquals("10:30", today.times.first())
        assertEquals(SchedulingService.OPENING_TIMES, days[1].times)
        assertFalse(days.any { it.date == "2026-10-17" || it.date == "2026-10-18" })
        assertEquals(10, days.size)
    }

    @Test
    fun bookingTakesTheSlotOfTheChosenProfessional() {
        // Arrange
        val request = request(professionalId = lucia, time = "14:00")

        // Act
        val result = service.book(maria, request)
        val days = service.availability(cardiology, lucia)!!
        val anyProfessional = service.availability(cardiology, professionalId = null)!!

        // Assert
        val created = assertIs<BookingResult.Created>(result).appointment
        assertEquals("Dra. Lúcia Oliveira", created.professionalName)
        assertEquals("Cardiologia", created.specialtyName)
        assertEquals("Graça", created.patientName)
        assertEquals(AppointmentStatus.Pending, created.status)
        assertFalse("14:00" in days.first { it.date == "2026-10-13" }.times)
        assertTrue("14:00" in anyProfessional.first { it.date == "2026-10-13" }.times)
        assertEquals(listOf(created), service.appointmentsOf(maria))
    }

    @Test
    fun theSameSlotCannotBeBookedTwice() {
        // Arrange
        service.book(maria, request(professionalId = lucia, time = "09:00"))

        // Act
        val result = service.book(ana, request(professionalId = lucia, time = "09:00"))

        // Assert
        assertIs<BookingResult.Conflict>(result)
        assertTrue(service.appointmentsOf(ana).isEmpty())
    }

    @Test
    fun cancellingFreesTheSlotForANewBooking() {
        // Arrange
        val first = assertIs<BookingResult.Created>(service.book(maria, request(time = "11:00"))).appointment
        service.cancel(maria, first.id)

        // Act
        val result = service.book(ana, request(time = "11:00"))

        // Assert
        assertIs<BookingResult.Created>(result)
        assertEquals(AppointmentStatus.Cancelled, service.appointmentsOf(maria).single().status)
        assertFalse("11:00" in service.availability(cardiology, lucia)!!.first { it.date == "2026-10-13" }.times)
    }

    @Test
    fun aPendingAppointmentCanBeCancelledEvenAfterItsTime() {
        // Arrange: Friday before the fixed clock, never confirmed by the clinic.
        val id = storeAppointment(maria, date = "2026-10-09", status = "pending")

        // Act
        val result = service.cancel(maria, id)

        // Assert
        assertEquals(AppointmentStatus.Cancelled, assertIs<CancelResult.Cancelled>(result).appointment.status)
    }

    @Test
    fun aConfirmedAppointmentCanOnlyBeCancelledBeforeItStarts() {
        // Arrange
        val past = storeAppointment(maria, date = "2026-10-12", time = "10:00", status = "confirmed")
        val future = storeAppointment(maria, date = "2026-10-12", time = "10:30", status = "confirmed")

        // Act
        val pastResult = service.cancel(maria, past)
        val futureResult = service.cancel(maria, future)

        // Assert
        assertIs<CancelResult.NotCancellable>(pastResult)
        assertIs<CancelResult.Cancelled>(futureResult)
        assertEquals(
            AppointmentStatus.Confirmed,
            service.appointmentsOf(maria).single { it.id == past }.status,
        )
    }

    @Test
    fun cancellingTwiceOrSomeoneElsesAppointmentIsRefused() {
        // Arrange
        val id = assertIs<BookingResult.Created>(service.book(maria, request(time = "16:00"))).appointment.id
        service.cancel(maria, id)

        // Act
        val again = service.cancel(maria, id)
        val byAna = service.cancel(ana, id)
        val unknown = service.cancel(maria, "missing")

        // Assert
        assertIs<CancelResult.NotCancellable>(again)
        assertEquals(CancelResult.NotFound, byAna)
        assertEquals(CancelResult.NotFound, unknown)
    }

    @Test
    fun anyProfessionalPicksTheOneStillFree() {
        // Arrange
        service.book(maria, request(professionalId = antonio, time = "15:00"))

        // Act
        val result = service.book(ana, request(professionalId = null, time = "15:00"))
        val days = service.availability(cardiology, professionalId = null)!!

        // Assert
        assertEquals("Dra. Lúcia Oliveira", assertIs<BookingResult.Created>(result).appointment.professionalName)
        assertFalse("15:00" in days.first { it.date == "2026-10-13" }.times)
    }

    @Test
    fun invalidRequestsAreRejected() {
        // Act
        val past = service.book(maria, request(date = "2026-10-12", time = "08:00"))
        val weekend = service.book(maria, request(date = "2026-10-17"))
        val tooFar = service.book(maria, request(date = "2026-11-30"))
        val closed = service.book(maria, request(time = "12:00"))
        val otherSpecialty = service.book(
            maria,
            request(professionalId = SchedulingCatalog.professionalId("Pediatria", "Dr. Marcos Souza")),
        )

        // Assert
        listOf(past, weekend, tooFar, closed, otherSpecialty).forEach { assertIs<BookingResult.Rejected>(it) }
        assertNull(service.availability("desconhecida", null))
        assertTrue(service.appointmentsOf(maria).isEmpty())
    }

    /** Writes an appointment straight to the table, for states booking cannot create. */
    private fun storeAppointment(
        patientId: String,
        date: String,
        time: String = "09:00",
        status: String,
    ): String {
        val id = UUID.randomUUID().toString()
        transaction(database) {
            AppointmentsTable.insert {
                it[AppointmentsTable.id] = id
                it[AppointmentsTable.patientId] = patientId
                it[professionalId] = lucia
                it[appointmentDate] = date
                it[appointmentTime] = time
                it[AppointmentsTable.status] = status
                it[reason] = ""
                it[notes] = ""
                it[createdAt] = 0
                it[slotKey] = AppointmentsTable.slotKeyOf(lucia, date, time)
            }
        }
        return id
    }

    private fun request(
        professionalId: String? = lucia,
        date: String = "2026-10-13",
        time: String = "10:00",
    ) = CreateAppointmentRequest(
        specialtyId = cardiology,
        professionalId = professionalId,
        date = date,
        time = time,
        reason = "Dor no peito ao subir escadas",
        notes = "",
    )

    private fun registration(email: String, socialName: String) = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = socialName,
        email = email,
        password = "segredo",
        primaryPhone = "(81) 99999-9999",
        primaryPhoneWhatsapp = true,
        secondaryPhone = "",
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = "",
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
}
