package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.common.security.SecretString
import grupotp.recifesaudavel.server.database.table.AppointmentsTable
import grupotp.recifesaudavel.server.database.table.ClinicsTable
import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import grupotp.recifesaudavel.server.database.table.PreTriagesTable
import grupotp.recifesaudavel.server.database.table.ProfessionalsTable
import grupotp.recifesaudavel.server.database.table.SessionsTable
import grupotp.recifesaudavel.server.database.table.SpecialtiesTable
import grupotp.recifesaudavel.server.database.table.UsersTable
import grupotp.recifesaudavel.server.database.entity.AppointmentEntity
import grupotp.recifesaudavel.server.database.entity.ProfessionalEntity
import grupotp.recifesaudavel.server.database.entity.UserEntity
import grupotp.recifesaudavel.server.repository.PreTriageRepository
import grupotp.recifesaudavel.server.repository.SchedulingRepository
import grupotp.recifesaudavel.server.repository.UserRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
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
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

class SchedulingServiceTest {
    private val recife = ZoneId.of("America/Recife")

    // Monday, 12 October 2026, 10:10 in Recife.
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 12, 10, 10, 0, 0, recife).toInstant(), recife)

    private lateinit var database: Database
    private lateinit var service: SchedulingService
    private lateinit var repository: SchedulingRepository
    private var maria = 0
    private var ana = 0
    private var cardiology = 0
    private var lucia = 0
    private var psychiatry = 0
    private var antonio = 0

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
            SchemaUtils.create(UsersTable, SessionsTable, ClinicsTable, SpecialtiesTable, ProfessionalsTable, AppointmentsTable, PreTriagesTable)
        }
        repository = SchedulingRepository(database).also { it.seedCatalogIfEmpty() }
        cardiology = specialty("Cardiologia")
        psychiatry = specialty("Psiquiatria")
        lucia = professional("Cardiologia", "Dra. Lúcia Oliveira")
        antonio = professional("Cardiologia", "Dr. Antônio Ferreira")
        service = SchedulingService(repository, clock, PreTriageRepository(database))
        val users = UserRepository(database)
        maria = users.insert(registration("maria@email.com", "Graça"), "hash", clock.instant().toKotlinInstant())!!.id
        ana = users.insert(registration("ana@email.com", ""), "hash", clock.instant().toKotlinInstant())!!.id
    }

    @AfterTest
    fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun availabilityOffersWeekdaysOnlyAndSkipsPastTimesOfToday() {
        // Act
        val days = service.availability(cardiology, professionalId = null)!!.single().days

        // Assert
        val today = days.first()
        assertEquals(LocalDate(2026, 10, 12), today.date)
        assertEquals(LocalTime(10, 30), today.times.first())
        assertEquals(SchedulingService.OPENING_TIMES, days[1].times)
        assertFalse(days.any { it.date == LocalDate(2026, 10, 17) || it.date == LocalDate(2026, 10, 18) })
        assertEquals(10, days.size)
    }

    @Test
    fun bookingTakesTheSlotOfTheChosenProfessional() {
        // Arrange
        val request = request(professionalId = lucia, time = "14:00")

        // Act
        val result = service.book(maria, request)
        val days = service.availability(cardiology, lucia)!!.single().days
        val anyProfessional = service.availability(cardiology, professionalId = null)!!.single().days

        // Assert
        val created = assertIs<BookingResult.Created>(result).appointment
        assertEquals("Dra. Lúcia Oliveira", created.professionalName)
        assertEquals("Cardiologia", created.specialtyName)
        assertEquals("Graça", created.patientName)
        assertEquals(AppointmentStatus.Pending, created.status)
        assertFalse(LocalTime.parse("14:00") in days.first { it.date == LocalDate(2026, 10, 13) }.times)
        assertTrue(LocalTime.parse("14:00") in anyProfessional.first { it.date == LocalDate(2026, 10, 13) }.times)
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
        assertFalse(LocalTime.parse("11:00") in service.availability(cardiology, lucia)!!.single().days.first { it.date == LocalDate(2026, 10, 13) }.times)
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
        val unknown = service.cancel(maria, -1)

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
        val days = service.availability(cardiology, professionalId = null)!!.single().days

        // Assert
        assertEquals("Dra. Lúcia Oliveira", assertIs<BookingResult.Created>(result).appointment.professionalName)
        assertFalse(LocalTime.parse("15:00") in days.first { it.date == LocalDate(2026, 10, 13) }.times)
    }

    @Test
    fun eachClinicOffersTimesInItsOwnTimeZone() {
        // Act: 10:10 in Recife is 09:10 in Manaus, one hour behind.
        val byClinic = service.availability(psychiatry, professionalId = null)!!.associateBy { it.clinicId }

        // Assert
        val recife = clinic("Recife")
        val manaus = clinic("Manaus")
        assertEquals(setOf(recife, manaus), byClinic.keys)
        assertEquals(LocalTime(10, 30), byClinic.getValue(recife).days.first().times.first())
        assertEquals(LocalTime(9, 30), byClinic.getValue(manaus).days.first().times.first())
    }

    @Test
    fun anyProfessionalNeedsTheClinicWhenTheSpecialtySpansZones() {
        // Arrange
        val manaus = clinic("Manaus")
        val base = request(professionalId = null, date = "2026-10-12", time = "09:30").copy(specialtyId = psychiatry)

        // Act
        val withoutClinic = service.book(maria, base)
        val inManaus = service.book(maria, base.copy(clinicId = manaus))

        // Assert: 09:30 is past in Recife (10:10) but still ahead in Manaus (09:10).
        assertIs<BookingResult.Rejected>(withoutClinic)
        val created = assertIs<BookingResult.Created>(inManaus).appointment
        assertEquals("Dr. Alexandre Nunes", created.professionalName)
        assertEquals("Manaus", created.clinic.city)
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
            request(professionalId = professional("Pediatria", "Dr. Marcos Souza")),
        )

        // Assert
        listOf(past, weekend, tooFar, closed, otherSpecialty).forEach { assertIs<BookingResult.Rejected>(it) }
        assertNull(service.availability(-1, null))
        assertTrue(service.appointmentsOf(maria).isEmpty())
    }

    /** Writes an appointment straight to the table, for states booking cannot create. */
    private fun storeAppointment(
        patientId: Int,
        date: String,
        time: String = "09:00",
        status: String,
    ): Int = transaction(database) {
        val day = LocalDate.parse(date)
        val hour = LocalTime.parse(time)
        AppointmentEntity.new {
            patient = UserEntity[patientId]
            professional = ProfessionalEntity[lucia]
            this.date = day
            this.time = hour
            this.status = status
            reason = ""
            notes = ""
            createdAt = Instant.fromEpochSeconds(0)
            slotKey = AppointmentsTable.slotKeyOf(lucia, day, hour)
        }.id.value
    }

    private fun specialty(name: String): Int = repository.specialties().first { it.name == name }.id

    private fun professional(specialty: String, name: String): Int =
        repository.professionals(specialty(specialty)).first { it.name == name }.id

    private fun clinic(city: String): Int = repository.clinics().first { it.city == city }.id

    private fun request(
        professionalId: Int? = lucia,
        date: String = "2026-10-13",
        time: String = "10:00",
    ) = CreateAppointmentRequest(
        specialtyId = cardiology,
        professionalId = professionalId,
        date = LocalDate.parse(date),
        time = LocalTime.parse(time),
        reason = "Dor no peito ao subir escadas",
        notes = "",
    )

    private fun registration(email: String, socialName: String) = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = socialName,
        email = email,
        password = SecretString("segredo"),
        primaryPhone = Phone.parse("(81) 99999-9999"),
        primaryPhoneWhatsapp = true,
        secondaryPhone = Phone.parse(""),
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = Phone.parse(""),
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
}
