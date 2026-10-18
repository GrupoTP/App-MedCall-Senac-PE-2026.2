package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.app.platform.loadTimeZones
import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private const val CARDIOLOGY = 1

class SchedulingUseCaseTest {
    private val account = AuthenticatedAccount(
        id = 1,
        fullName = "Maria das Graças Silva",
        socialName = "Graça",
        email = "maria@email.com",
        sessionToken = SecretString("token-da-maria"),
    )

    @Test
    fun listSavesTheServerListOnTheDevice() = runTest {
        // Arrange
        val gateway = FakeAppointmentGateway(list = SchedulingResult.Success(listOf(later, sooner)))
        val local = FakeLocalAppointments()

        // Act
        val snapshot = ListAppointments(gateway, local)(account)

        // Assert
        assertEquals("token-da-maria", gateway.lastToken)
        assertFalse(snapshot.offline)
        assertNull(snapshot.error)
        assertEquals(listOf(sooner, later), snapshot.appointments)
        assertEquals(listOf(sooner, later), local.saved[1])
    }

    @Test
    fun offlineListShowsTheCopySavedOnTheDevice() = runTest {
        // Arrange
        val gateway = FakeAppointmentGateway(list = SchedulingResult.Failed(SchedulingError.Offline))
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(sooner)))

        // Act
        val snapshot = ListAppointments(gateway, local)(account)

        // Assert
        assertTrue(snapshot.offline)
        assertNull(snapshot.error)
        assertEquals(listOf(sooner), snapshot.appointments)
    }

    @Test
    fun expiredSessionKeepsTheDeviceCopyAndReportsIt() = runTest {
        // Arrange
        val gateway = FakeAppointmentGateway(list = SchedulingResult.Failed(SchedulingError.SessionExpired))
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(sooner)))

        // Act
        val snapshot = ListAppointments(gateway, local)(account)

        // Assert
        assertFalse(snapshot.offline)
        assertEquals(SchedulingError.SessionExpired, snapshot.error)
        assertEquals(listOf(sooner), snapshot.appointments)
        assertEquals(SESSION_EXPIRED_MESSAGE, snapshot.error?.toMessage())
    }

    @Test
    fun anotherAccountNeverSeesTheSavedList() = runTest {
        // Arrange
        val gateway = FakeAppointmentGateway(list = SchedulingResult.Failed(SchedulingError.Offline))
        val local = FakeLocalAppointments(mutableMapOf(2 to listOf(sooner)))

        // Act
        val snapshot = ListAppointments(gateway, local)(account)

        // Assert
        assertTrue(snapshot.appointments.isEmpty())
    }

    @Test
    fun bookingAddsTheStoredAppointmentToTheDeviceCopy() = runTest {
        // Arrange
        val gateway = FakeAppointmentGateway(book = SchedulingResult.Success(sooner))
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(later)))

        // Act
        val result = BookAppointment(gateway, local)(account, request)

        // Assert
        assertEquals(SchedulingResult.Success(sooner), result)
        assertEquals(request, gateway.booked)
        assertEquals(listOf(sooner, later), local.saved[1])
    }

    @Test
    fun failedBookingNeverTouchesTheDeviceCopy() = runTest {
        // Arrange
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(later)))
        val offline = FakeAppointmentGateway(book = SchedulingResult.Failed(SchedulingError.Offline))
        val conflict = FakeAppointmentGateway(
            book = SchedulingResult.Failed(SchedulingError.Rejected("Este horário acabou de ser ocupado.")),
        )

        // Act
        val offlineResult = BookAppointment(offline, local)(account, request)
        val conflictResult = BookAppointment(conflict, local)(account, request)

        // Assert
        assertEquals(SCHEDULING_OFFLINE_MESSAGE, assertIs<SchedulingResult.Failed>(offlineResult).error.toMessage())
        assertIs<SchedulingResult.Failed>(conflictResult)
        assertEquals(listOf(later), local.saved[1])
        assertEquals(0, local.saves)
    }

    @Test
    fun cancellingReplacesTheDeviceCopyOnlyAfterTheServerAccepted() = runTest {
        // Arrange
        val cancelled = later.copy(status = AppointmentStatus.Cancelled)
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(sooner, later)))
        val accepted = FakeAppointmentGateway(cancel = SchedulingResult.Success(cancelled))
        val offline = FakeAppointmentGateway()

        // Act
        val offlineResult = CancelAppointment(offline, local)(account, later.id)
        val savesAfterOffline = local.saves
        val acceptedResult = CancelAppointment(accepted, local)(account, later.id)

        // Assert
        assertEquals(
            CANCELLATION_OFFLINE_MESSAGE,
            assertIs<SchedulingResult.Failed>(offlineResult).error.toCancellationMessage(),
        )
        assertEquals(0, savesAfterOffline)
        assertEquals(SchedulingResult.Success(cancelled), acceptedResult)
        assertEquals(listOf(sooner, cancelled), local.saved[1])
    }

    @Test
    fun cancellationErrorMessages() {
        // Arrange
        val offline = SchedulingError.Offline
        val sessionExpired = SchedulingError.SessionExpired
        val rejected = SchedulingError.Rejected("Erro na clínica.")

        // Act
        val offlineMessage = offline.toCancellationMessage()
        val sessionExpiredMessage = sessionExpired.toCancellationMessage()
        val rejectedMessage = rejected.toCancellationMessage()

        // Assert
        assertEquals(CANCELLATION_OFFLINE_MESSAGE, offlineMessage)
        assertEquals(SESSION_EXPIRED_MESSAGE, sessionExpiredMessage)
        assertEquals("Erro na clínica.", rejectedMessage)
    }

    @Test
    fun optionsNeedTheServer() = runTest {
        // Arrange
        val online = FakeAppointmentGateway()
        val offline = FakeAppointmentGateway(specialties = SchedulingResult.Failed(SchedulingError.Offline))

        // Act
        val loaded = LoadSchedulingOptions(online)(account)
        val failed = LoadSchedulingOptions(offline)(account)

        // Assert
        val options = assertIs<SchedulingResult.Success<SchedulingOptions>>(loaded).value
        assertEquals(listOf(lucia), options.professionalsOf(CARDIOLOGY))
        assertEquals("Recife", options.clinic(lucia.clinicId)?.city)
        assertEquals(SchedulingResult.Failed(SchedulingError.Offline), failed)
    }

    @Test
    fun upcomingSkipsPastAndCancelledAppointments() {
        // Arrange
        val past = sooner.copy(id = 90, date = LocalDate(2026, 10, 1))
        val cancelled = later.copy(id = 91, status = AppointmentStatus.Cancelled)

        // Act
        val upcoming = upcomingAppointments(listOf(later, cancelled, past, sooner), now = Instant.parse("2026-10-13T11:00:00Z"))

        // Assert
        assertEquals(listOf(sooner, later), upcoming)
    }

    @Test
    fun appointmentsInOtherZonesArePlacedByTheirOwnClinicClock() {
        // Arrange: 09:00 in Manaus is 13:00 UTC, after 09:30 in Recife (12:30 UTC).
        loadTimeZones()
        val recifeMorning = appointment(id = 3, date = "2026-10-14", time = "09:30")
        val manausMorning = appointment(id = 4, date = "2026-10-14", time = "09:00", clinic = manaus)

        // Act
        val bothAhead = upcomingAppointments(listOf(manausMorning, recifeMorning), now = Instant.parse("2026-10-14T12:00:00Z"))
        val onlyManaus = upcomingAppointments(listOf(manausMorning, recifeMorning), now = Instant.parse("2026-10-14T12:45:00Z"))

        // Assert
        assertEquals(listOf(recifeMorning, manausMorning), bothAhead)
        assertEquals(listOf(manausMorning), onlyManaus)
    }

    private val recife = Clinic(id = 1, name = "Clínica Boa Vista", city = "Recife", timeZoneId = "America/Recife")
    private val manaus = Clinic(id = 2, name = "Teleconsulta Manaus", city = "Manaus", timeZoneId = "America/Manaus")
    private val sooner = appointment(id = 1, date = "2026-10-14", time = "09:00")
    private val later = appointment(id = 2, date = "2026-10-20", time = "14:30")
    private val lucia = Professional(id = 10, name = "Dra. Lúcia Oliveira", specialtyId = CARDIOLOGY, clinicId = recife.id)
    private val request = CreateAppointmentRequest(
        specialtyId = CARDIOLOGY,
        professionalId = null,
        date = LocalDate(2026, 10, 14),
        time = LocalTime(9, 0),
        reason = "",
        notes = "",
    )

    private fun appointment(id: Int, date: String, time: String, clinic: Clinic = recife) = Appointment(
        id = id,
        specialtyName = "Cardiologia",
        professionalName = "Dra. Lúcia Oliveira",
        clinic = clinic,
        date = LocalDate.parse(date),
        time = LocalTime.parse(time),
        status = AppointmentStatus.Pending,
        patientName = "Graça",
        contactPhone = Phone.parse("(81) 99999-9999"),
    )

    private inner class FakeAppointmentGateway(
        private val specialties: SchedulingResult<List<Specialty>> =
            SchedulingResult.Success(listOf(Specialty(id = CARDIOLOGY, name = "Cardiologia"))),
        private val list: SchedulingResult<List<Appointment>> = SchedulingResult.Success(emptyList()),
        private val book: SchedulingResult<Appointment> = SchedulingResult.Failed(SchedulingError.Offline),
        private val cancel: SchedulingResult<Appointment> = SchedulingResult.Failed(SchedulingError.Offline),
    ) : AppointmentGateway {
        var lastToken: String? = null
        var booked: CreateAppointmentRequest? = null

        override suspend fun specialties(token: SecretString) = specialties

        override suspend fun professionals(token: SecretString): SchedulingResult<List<Professional>> {
            lastToken = token.value
            return SchedulingResult.Success(listOf(lucia, Professional(id = 11, name = "Dr. Marcos Souza", specialtyId = 2, clinicId = recife.id)))
        }

        override suspend fun clinics(token: SecretString): SchedulingResult<List<Clinic>> =
            SchedulingResult.Success(listOf(recife, manaus))

        override suspend fun availability(
            token: SecretString,
            specialtyId: Int,
            professionalId: Int?,
        ): SchedulingResult<List<ClinicAvailability>> = SchedulingResult.Success(emptyList())

        override suspend fun book(token: SecretString, request: CreateAppointmentRequest): SchedulingResult<Appointment> {
            lastToken = token.value
            booked = request
            return book
        }

        override suspend fun list(token: SecretString): SchedulingResult<List<Appointment>> {
            lastToken = token.value
            return list
        }

        override suspend fun cancel(token: SecretString, appointmentId: Int): SchedulingResult<Appointment> {
            lastToken = token.value
            return cancel
        }
    }
}

private class FakeLocalAppointments(
    val saved: MutableMap<Int, List<Appointment>> = mutableMapOf(),
) : LocalAppointmentGateway {
    var saves = 0

    override suspend fun saveAppointments(accountId: Int, appointments: List<Appointment>) {
        saves += 1
        saved.clear()
        saved[accountId] = appointments
    }

    override suspend fun loadAppointments(accountId: Int): List<Appointment>? = saved[accountId]
}
