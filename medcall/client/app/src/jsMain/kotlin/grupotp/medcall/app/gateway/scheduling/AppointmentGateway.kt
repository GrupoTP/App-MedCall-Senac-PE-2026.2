package grupotp.medcall.app.gateway.scheduling

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.common.scheduling.Appointment
import grupotp.medcall.common.scheduling.Clinic
import grupotp.medcall.common.scheduling.ClinicAvailability
import grupotp.medcall.common.scheduling.CreateAppointmentRequest
import grupotp.medcall.common.scheduling.Professional
import grupotp.medcall.common.scheduling.Specialty
import grupotp.medcall.common.security.SecretString

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
