package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AvailableDay
import grupotp.recifesaudavel.common.CreateAppointmentRequest
import grupotp.recifesaudavel.common.Professional
import grupotp.recifesaudavel.common.Specialty

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
 * Appointment API of the server. Every call except [specialties] needs the session token.
 */
interface AppointmentGateway {
    suspend fun specialties(): SchedulingResult<List<Specialty>>

    suspend fun professionals(token: String): SchedulingResult<List<Professional>>

    suspend fun availability(token: String, specialtyId: String, professionalId: String?): SchedulingResult<List<AvailableDay>>

    suspend fun book(token: String, request: CreateAppointmentRequest): SchedulingResult<Appointment>

    suspend fun list(token: String): SchedulingResult<List<Appointment>>
}

/**
 * Copy of the signed-in person's appointments kept on the device, so they can be read offline.
 * Only one account is kept: saving for one account drops any other.
 */
interface LocalAppointmentGateway {
    suspend fun saveAppointments(accountId: String, appointments: List<Appointment>)

    suspend fun loadAppointments(accountId: String): List<Appointment>?
}
