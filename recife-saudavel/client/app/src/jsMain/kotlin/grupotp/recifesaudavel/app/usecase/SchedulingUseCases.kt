package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AppointmentStatus
import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.AvailableDay
import grupotp.recifesaudavel.common.CreateAppointmentRequest
import grupotp.recifesaudavel.common.Professional
import grupotp.recifesaudavel.common.Specialty

data class SchedulingOptions(
    val specialties: List<Specialty>,
    val professionals: List<Professional>,
) {
    fun professionalsOf(specialtyId: String): List<Professional> = professionals.filter { it.specialtyId == specialtyId }
}

/**
 * Specialties and professionals for the first step. Loading them also tells whether the server
 * can be reached, so the flow never opens offline.
 */
class LoadSchedulingOptions(
    private val gateway: AppointmentGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): SchedulingResult<SchedulingOptions> {
        val specialties = when (val result = gateway.specialties()) {
            is SchedulingResult.Success -> result.value
            is SchedulingResult.Failed -> return result
        }
        return when (val result = gateway.professionals(account.sessionToken)) {
            is SchedulingResult.Success -> SchedulingResult.Success(SchedulingOptions(specialties, result.value))
            is SchedulingResult.Failed -> result
        }
    }
}

/**
 * Open days and times, always asked to the server (RN-009).
 */
class LoadAvailability(
    private val gateway: AppointmentGateway,
) {
    suspend operator fun invoke(
        account: AuthenticatedAccount,
        specialtyId: String,
        professionalId: String?,
    ): SchedulingResult<List<AvailableDay>> = gateway.availability(account.sessionToken, specialtyId, professionalId)
}

/**
 * Books on the server. The local copy only changes after the server stored the appointment, so a
 * failure never shows up as a booked appointment (RNF-012).
 */
class BookAppointment(
    private val gateway: AppointmentGateway,
    private val local: LocalAppointmentGateway,
) {
    suspend operator fun invoke(
        account: AuthenticatedAccount,
        request: CreateAppointmentRequest,
    ): SchedulingResult<Appointment> {
        val result = gateway.book(account.sessionToken, request)
        if (result is SchedulingResult.Success) {
            runCatching {
                val saved = local.loadAppointments(account.id).orEmpty().filter { it.id != result.value.id }
                local.saveAppointments(account.id, sortedByTime(saved + result.value))
            }
        }
        return result
    }
}

/**
 * Cancels on the server. Like booking, the device copy only changes after the server accepted it.
 */
class CancelAppointment(
    private val gateway: AppointmentGateway,
    private val local: LocalAppointmentGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount, appointmentId: String): SchedulingResult<Appointment> {
        val result = gateway.cancel(account.sessionToken, appointmentId)
        if (result is SchedulingResult.Success) {
            runCatching {
                val saved = local.loadAppointments(account.id).orEmpty().filter { it.id != result.value.id }
                local.saveAppointments(account.id, sortedByTime(saved + result.value))
            }
        }
        return result
    }
}

/**
 * @property offline true when the list came from the device because the server was unreachable.
 * @property error why the server refused the list, if it did. The device copy is still shown.
 */
data class AppointmentsSnapshot(
    val appointments: List<Appointment>,
    val offline: Boolean,
    val error: SchedulingError? = null,
)

/**
 * Server first, device copy when offline. Each fresh list replaces the device copy.
 */
class ListAppointments(
    private val gateway: AppointmentGateway,
    private val local: LocalAppointmentGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): AppointmentsSnapshot =
        when (val result = gateway.list(account.sessionToken)) {
            is SchedulingResult.Success -> {
                val appointments = sortedByTime(result.value)
                runCatching { local.saveAppointments(account.id, appointments) }
                AppointmentsSnapshot(appointments, offline = false)
            }
            is SchedulingResult.Failed -> AppointmentsSnapshot(
                appointments = runCatching { local.loadAppointments(account.id) }.getOrNull().orEmpty(),
                offline = result.error == SchedulingError.Offline,
                error = result.error.takeUnless { it == SchedulingError.Offline },
            )
        }
}

/**
 * Forgets the signed-in profile and its appointments on this device.
 */
class SignOut(
    private val accounts: LocalAccountGateway,
    private val remote: AccountGateway,
) {
    /**
     * @param token session to revoke on the server too. The server is asked first and a failure
     * (offline, already expired) never stops the device from forgetting the profile.
     */
    suspend operator fun invoke(token: String? = null) {
        if (!token.isNullOrBlank()) {
            runCatching { remote.signOut(token) }
        }
        accounts.clear()
    }
}

/**
 * Appointments not cancelled and not yet past, soonest first.
 *
 * @param now local date and time as `yyyy-MM-ddTHH:mm`.
 */
fun upcomingAppointments(appointments: List<Appointment>, now: String): List<Appointment> =
    sortedByTime(appointments).filter { it.status != AppointmentStatus.Cancelled && "${it.date}T${it.time}" >= now }

private fun sortedByTime(appointments: List<Appointment>): List<Appointment> =
    appointments.sortedWith(compareBy({ it.date }, { it.time }))

const val SCHEDULING_OFFLINE_MESSAGE =
    "O agendamento precisa de internet. Conecte-se e tente de novo. Nenhuma consulta foi marcada."

const val CANCELLATION_OFFLINE_MESSAGE =
    "O cancelamento precisa de internet. Conecte-se e tente de novo. Nenhuma consulta foi cancelada."

const val SESSION_EXPIRED_MESSAGE = "Sua sessão expirou. Entre de novo para continuar."

fun SchedulingError.toMessage(): String = when (this) {
    SchedulingError.Offline -> SCHEDULING_OFFLINE_MESSAGE
    SchedulingError.SessionExpired -> SESSION_EXPIRED_MESSAGE
    is SchedulingError.Rejected -> message
}

fun SchedulingError.toCancellationMessage(): String = when (this) {
    SchedulingError.Offline -> CANCELLATION_OFFLINE_MESSAGE
    SchedulingError.SessionExpired -> SESSION_EXPIRED_MESSAGE
    is SchedulingError.Rejected -> message
}

