package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import grupotp.recifesaudavel.common.security.SecretString

sealed interface SchedulingResult<out T> {
    data class Success<T>(val value: T) : SchedulingResult<T>

    data class Failed(val error: SchedulingError) : SchedulingResult<Nothing>
}

sealed interface SchedulingError {
    /** The server could not be reached. Scheduling needs the network. */
    data object Offline : SchedulingError

    /** The saved token is missing or no longer valid. The person must sign in again. */
    data object SessionExpired : SchedulingError

    data class Rejected(val message: String) : SchedulingError
}

/**
 * Appointment API of the server. Every call needs the session token.
 */
interface AppointmentGateway {
    suspend fun specialties(token: SecretString): SchedulingResult<List<Specialty>>

    suspend fun professionals(token: SecretString): SchedulingResult<List<Professional>>

    suspend fun clinics(token: SecretString): SchedulingResult<List<Clinic>>

    suspend fun availability(token: SecretString, specialtyId: String, professionalId: String?): SchedulingResult<List<ClinicAvailability>>

    suspend fun book(token: SecretString, request: CreateAppointmentRequest): SchedulingResult<Appointment>

    suspend fun list(token: SecretString): SchedulingResult<List<Appointment>>

    suspend fun cancel(token: SecretString, appointmentId: String): SchedulingResult<Appointment>
}

/**
 * Copy of the signed-in person's appointments kept on the device, so they can be read offline.
 * Only one account is kept: saving for one account drops any other.
 */
interface LocalAppointmentGateway {
    suspend fun saveAppointments(accountId: String, appointments: List<Appointment>)

    suspend fun loadAppointments(accountId: String): List<Appointment>?
}
