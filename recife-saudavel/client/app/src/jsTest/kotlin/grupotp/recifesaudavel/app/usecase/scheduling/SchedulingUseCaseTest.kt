package grupotp.recifesaudavel.app.usecase.scheduling

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingOptions
import grupotp.recifesaudavel.app.fake.SampleAppointments.later
import grupotp.recifesaudavel.app.fake.SampleAppointments.manaus
import grupotp.recifesaudavel.app.fake.SampleAppointments.recife
import grupotp.recifesaudavel.app.fake.SampleAppointments.sooner
import grupotp.recifesaudavel.app.gateway.local.LocalAppointmentGateway
import grupotp.recifesaudavel.app.gateway.scheduling.AppointmentGateway
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

private const val CARDIOLOGY = 1

class SchedulingUseCaseTest {
    private val account = AuthenticatedAccount(
        id = 1,
        fullName = "Maria das Graças Silva",
        socialName = "Graça",
        email = "maria@email.com",
        sessionToken = SecretString("token-da-maria"),
    )
    private val lucia = Professional(id = 10, name = "Dra. Lúcia Oliveira", specialtyId = CARDIOLOGY, clinicId = recife.id)
    private val request = CreateAppointmentRequest(
        specialtyId = CARDIOLOGY,
        professionalId = null,
        date = LocalDate(2026, 10, 14),
        time = LocalTime(9, 0),
        reason = "",
        notes = "",
    )

    @Test
    fun listSavesTheServerListOnTheDevice() = runTest {
        // Arrange
        val gateway = FakeAppointmentGateway(list = RemoteResult.Success(listOf(later, sooner)))
        val local = FakeLocalAppointments()

        // Act
        val snapshot = ListAppointmentsUseCase(gateway, local)(account)

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
        val gateway = FakeAppointmentGateway(list = RemoteResult.Failed(RemoteError.Offline))
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(sooner)))

        // Act
        val snapshot = ListAppointmentsUseCase(gateway, local)(account)

        // Assert
        assertTrue(snapshot.offline)
        assertNull(snapshot.error)
        assertEquals(listOf(sooner), snapshot.appointments)
    }

    @Test
    fun expiredSessionKeepsTheDeviceCopyAndReportsIt() = runTest {
        // Arrange
        val gateway = FakeAppointmentGateway(list = RemoteResult.Failed(RemoteError.SessionExpired))
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(sooner)))

        // Act
        val snapshot = ListAppointmentsUseCase(gateway, local)(account)

        // Assert
        assertFalse(snapshot.offline)
        assertEquals(RemoteError.SessionExpired, snapshot.error)
        assertEquals(listOf(sooner), snapshot.appointments)
    }

    @Test
    fun anotherAccountNeverSeesTheSavedList() = runTest {
        // Arrange
        val gateway = FakeAppointmentGateway(list = RemoteResult.Failed(RemoteError.Offline))
        val local = FakeLocalAppointments(mutableMapOf(2 to listOf(sooner)))

        // Act
        val snapshot = ListAppointmentsUseCase(gateway, local)(account)

        // Assert
        assertTrue(snapshot.appointments.isEmpty())
    }

    @Test
    fun bookingAddsTheStoredAppointmentToTheDeviceCopy() = runTest {
        // Arrange
        val gateway = FakeAppointmentGateway(book = RemoteResult.Success(sooner))
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(later)))

        // Act
        val result = BookAppointmentUseCase(gateway, local)(account, request)

        // Assert
        assertEquals(RemoteResult.Success(sooner), result)
        assertEquals(request, gateway.booked)
        assertEquals(listOf(sooner, later), local.saved[1])
    }

    @Test
    fun failedBookingNeverTouchesTheDeviceCopy() = runTest {
        // Arrange
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(later)))
        val offline = FakeAppointmentGateway(book = RemoteResult.Failed(RemoteError.Offline))
        val conflict = FakeAppointmentGateway(
            book = RemoteResult.Failed(RemoteError.Rejected("Este horário acabou de ser ocupado.")),
        )

        // Act
        val offlineResult = BookAppointmentUseCase(offline, local)(account, request)
        val conflictResult = BookAppointmentUseCase(conflict, local)(account, request)

        // Assert
        assertEquals(RemoteError.Offline, assertIs<RemoteResult.Failed>(offlineResult).error)
        assertIs<RemoteResult.Failed>(conflictResult)
        assertEquals(listOf(later), local.saved[1])
        assertEquals(0, local.saves)
    }

    @Test
    fun cancellingReplacesTheDeviceCopyOnlyAfterTheServerAccepted() = runTest {
        // Arrange
        val cancelled = later.copy(status = AppointmentStatus.Cancelled)
        val local = FakeLocalAppointments(mutableMapOf(1 to listOf(sooner, later)))
        val accepted = FakeAppointmentGateway(cancel = RemoteResult.Success(cancelled))
        val offline = FakeAppointmentGateway()

        // Act
        val offlineResult = CancelAppointmentUseCase(offline, local)(account, later.id)
        val savesAfterOffline = local.saves
        val acceptedResult = CancelAppointmentUseCase(accepted, local)(account, later.id)

        // Assert
        assertEquals(RemoteError.Offline, assertIs<RemoteResult.Failed>(offlineResult).error)
        assertEquals(0, savesAfterOffline)
        assertEquals(RemoteResult.Success(cancelled), acceptedResult)
        assertEquals(listOf(sooner, cancelled), local.saved[1])
    }

    @Test
    fun optionsNeedTheServer() = runTest {
        // Arrange
        val online = FakeAppointmentGateway()
        val offline = FakeAppointmentGateway(specialties = RemoteResult.Failed(RemoteError.Offline))

        // Act
        val loaded = LoadSchedulingOptionsUseCase(online)(account)
        val failed = LoadSchedulingOptionsUseCase(offline)(account)

        // Assert
        val options = assertIs<RemoteResult.Success<SchedulingOptions>>(loaded).value
        assertEquals(listOf(lucia), options.professionalsOf(CARDIOLOGY))
        assertEquals("Recife", options.clinic(lucia.clinicId)?.city)
        assertEquals(RemoteResult.Failed(RemoteError.Offline), failed)
    }

    private inner class FakeAppointmentGateway(
        private val specialties: RemoteResult<List<Specialty>> =
            RemoteResult.Success(listOf(Specialty(id = CARDIOLOGY, name = "Cardiologia"))),
        private val list: RemoteResult<List<Appointment>> = RemoteResult.Success(emptyList()),
        private val book: RemoteResult<Appointment> = RemoteResult.Failed(RemoteError.Offline),
        private val cancel: RemoteResult<Appointment> = RemoteResult.Failed(RemoteError.Offline),
    ) : AppointmentGateway {
        var lastToken: String? = null
        var booked: CreateAppointmentRequest? = null

        override suspend fun specialties(token: SecretString) = specialties

        override suspend fun professionals(token: SecretString): RemoteResult<List<Professional>> {
            lastToken = token.value
            return RemoteResult.Success(listOf(lucia, Professional(id = 11, name = "Dr. Marcos Souza", specialtyId = 2, clinicId = recife.id)))
        }

        override suspend fun clinics(token: SecretString): RemoteResult<List<Clinic>> =
            RemoteResult.Success(listOf(recife, manaus))

        override suspend fun availability(
            token: SecretString,
            specialtyId: Int,
            professionalId: Int?,
        ): RemoteResult<List<ClinicAvailability>> = RemoteResult.Success(emptyList())

        override suspend fun book(token: SecretString, request: CreateAppointmentRequest): RemoteResult<Appointment> {
            lastToken = token.value
            booked = request
            return book
        }

        override suspend fun list(token: SecretString): RemoteResult<List<Appointment>> {
            lastToken = token.value
            return list
        }

        override suspend fun cancel(token: SecretString, appointmentId: Int): RemoteResult<Appointment> {
            lastToken = token.value
            return cancel
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
}
