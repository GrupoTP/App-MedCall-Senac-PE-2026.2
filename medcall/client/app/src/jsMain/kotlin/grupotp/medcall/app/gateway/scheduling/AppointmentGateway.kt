package grupotp.recifesaudavel.app.gateway.scheduling

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Appointment API of the server. Every call needs the session token.
 */
interface AppointmentGateway {
    suspend fun specialties(token: SecretString): RemoteResult<List<Specialty>>

    suspend fun professionals(token: SecretString): RemoteResult<List<Professional>>

    suspend fun clinics(token: SecretString): RemoteResult<List<Clinic>>

    suspend fun availability(token: SecretString, specialtyId: Int, professionalId: Int?): RemoteResult<List<ClinicAvailability>>

    suspend fun book(token: SecretString, request: CreateAppointmentRequest): RemoteResult<Appointment>

    suspend fun list(token: SecretString): RemoteResult<List<Appointment>>

    suspend fun cancel(token: SecretString, appointmentId: Int): RemoteResult<Appointment>
}
