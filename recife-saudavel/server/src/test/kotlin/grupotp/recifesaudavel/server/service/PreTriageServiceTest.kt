package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.phone.Phone
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.pretriage.SymptomsStart
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.common.security.SecretString
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreTriageServiceTest {
    private val recife = ZoneId.of("America/Recife")

    // Monday, 12 October 2026, 10:10 in Recife.
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 12, 10, 10, 0, 0, recife).toInstant(), recife)
    private val cardiology = SchedulingCatalog.specialtyId("Cardiologia")
    private val lucia = SchedulingCatalog.professionalId("Cardiologia", "Dra. Lúcia Oliveira")

    private lateinit var database: Database
    private lateinit var service: PreTriageService
    private lateinit var scheduling: SchedulingService
    private lateinit var users: UserRepository
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
        val appointments = SchedulingRepository(database).also { it.seedCatalogIfEmpty() }
        val preTriages = PreTriageRepository(database)
        users = UserRepository(database)
        service = PreTriageService(preTriages, users, appointments, clock)
        scheduling = SchedulingService(appointments, clock, preTriages)
        maria = users.insert(UUID.randomUUID().toString(), registration("maria@email.com"), "hash")!!.id
        ana = users.insert(UUID.randomUUID().toString(), registration("ana@email.com"), "hash")!!.id
    }

    @AfterTest
    fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun firstDraftComesFromTheProfile() {
        // Act
        val draft = service.draft(maria)!!

        // Assert
        assertEquals("Maria das Graças Silva", draft.fullName)
        assertEquals("Graça", draft.socialName)
        assertEquals("81999999999", draft.primaryPhone.digits)
        assertEquals(true, draft.primaryPhoneWhatsapp)
        assertNull(draft.birthDate)
        assertEquals("", draft.allergies)
    }

    @Test
    fun nextDraftKeepsHistoryAndMedicationsButNotTheComplaintNorEditedPersonalData() {
        // Arrange
        service.submit(maria, form(fullName = "Outro Nome", primaryPhone = "(81) 3333-3333"))

        // Act
        val draft = service.draft(maria)!!

        // Assert
        assertEquals("Maria das Graças Silva", draft.fullName)
        assertEquals("81999999999", draft.primaryPhone.digits)
        assertEquals(LocalDate(1980, 5, 31), draft.birthDate)
        assertEquals("Hipertensão", draft.previousConditions)
        assertEquals("Dipirona", draft.allergies)
        assertEquals("Losartana 50mg", draft.regularMedications)
        assertEquals("Vitamina D", draft.supplements)
        assertEquals("", draft.reason)
        assertNull(draft.intensity)
    }

    @Test
    fun symptomsStartIsStoredAsItsCode() {
        // Arrange
        val sent = assertIs<PreTriageResult.Created>(service.submit(maria, form().copy(symptomsStart = SymptomsStart.TwoWeeks)))

        // Act
        val stored = transaction(database) {
            PreTriagesTable.selectAll().where { PreTriagesTable.id eq sent.preTriage.id }.single()[PreTriagesTable.form]
        }
        val pending = service.pending(maria).preTriage!!

        // Assert
        assertTrue("\"symptomsStart\":\"two_weeks\"" in stored)
        assertEquals(SymptomsStart.TwoWeeks, pending.form.symptomsStart)
    }

    @Test
    fun anOldFreeTextSymptomsStartReadsAsUnanswered() {
        // Arrange: stored before SymptomsStart existed, with the option's text.
        transaction(database) {
            PreTriagesTable.insert {
                it[id] = UUID.randomUUID().toString()
                it[patientId] = maria
                it[form] = """{"fullName":"Maria","reason":"Dor","symptomsStart":"Há 2 semanas"}"""
                it[createdAt] = 0
            }
        }

        // Act
        val pending = service.pending(maria).preTriage!!

        // Assert
        assertEquals("Dor", pending.form.reason)
        assertNull(pending.form.symptomsStart)
    }

    @Test
    fun invalidFormsAreRejected() {
        // Act
        val results = listOf(
            form(fullName = " "),
            form(birthDate = null),
            form(birthDate = LocalDate(1899, 12, 31)),
            form(birthDate = LocalDate(2027, 1, 1)),
            form(primaryPhone = ""),
            form(reason = ""),
            form(intensity = 0),
            form(intensity = 11),
        ).map { service.submit(maria, it) }

        // Assert
        results.forEach { assertIs<PreTriageResult.Rejected>(it) }
        assertNull(service.pending(maria).preTriage)
    }

    @Test
    fun theLatestPreTriageGoesWithTheNextBookingOnly() {
        // Arrange
        val sent = assertIs<PreTriageResult.Created>(service.submit(maria, form())).preTriage

        // Act
        val pendingBefore = service.pending(maria).preTriage
        val booked = scheduling.book(maria, request("09:00", sent.id))
        val pendingAfter = service.pending(maria).preTriage
        val second = scheduling.book(maria, request("10:00", sent.id))

        // Assert
        assertEquals(sent.id, pendingBefore?.id)
        assertEquals(sent.id, assertIs<BookingResult.Created>(booked).appointment.preTriageId)
        assertNull(pendingAfter)
        assertIs<BookingResult.Rejected>(second)
    }

    @Test
    fun bookingWithoutThePreTriageKeepsItForLater() {
        // Arrange
        val sent = assertIs<PreTriageResult.Created>(service.submit(maria, form())).preTriage

        // Act
        val booked = scheduling.book(maria, request("09:00", preTriageId = null))

        // Assert
        assertNull(assertIs<BookingResult.Created>(booked).appointment.preTriageId)
        assertEquals(sent.id, service.pending(maria).preTriage?.id)
    }

    @Test
    fun cancellingTheAppointmentFreesThePreTriage() {
        // Arrange
        val sent = assertIs<PreTriageResult.Created>(service.submit(maria, form())).preTriage
        val booked = assertIs<BookingResult.Created>(scheduling.book(maria, request("09:00", sent.id))).appointment

        // Act
        scheduling.cancel(maria, booked.id)

        // Assert
        assertEquals(sent.id, service.pending(maria).preTriage?.id)
    }

    @Test
    fun anotherPatientCannotAttachThePreTriage() {
        // Arrange
        val sent = assertIs<PreTriageResult.Created>(service.submit(maria, form())).preTriage

        // Act
        val result = scheduling.book(ana, request("09:00", sent.id))

        // Assert
        assertIs<BookingResult.Rejected>(result)
        assertNull(service.pending(ana).preTriage)
    }

    private fun form(
        fullName: String = "Maria das Graças Silva",
        birthDate: LocalDate? = LocalDate(1980, 5, 31),
        primaryPhone: String = "(81) 99999-9999",
        reason: String = "Dor no peito ao subir escadas",
        intensity: Int? = 6,
    ) = PreTriageForm(
        fullName = fullName,
        birthDate = birthDate,
        primaryPhone = Phone.parse(primaryPhone),
        reason = reason,
        intensity = intensity,
        previousConditions = "Hipertensão",
        allergies = "Dipirona",
        regularMedications = "Losartana 50mg",
        supplements = "Vitamina D",
    )

    private fun request(time: String, preTriageId: String?) = CreateAppointmentRequest(
        specialtyId = cardiology,
        professionalId = lucia,
        date = LocalDate(2026, 10, 13),
        time = LocalTime.parse(time),
        reason = "Dor no peito ao subir escadas",
        notes = "",
        preTriageId = preTriageId,
    )

    private fun registration(email: String) = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = "Graça",
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
