package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.domain.scheduling.AppointmentGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.serialization.json.Json

class RestAppointmentGateway(
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AppointmentGateway {
    override suspend fun specialties(token: SecretString): SchedulingResult<List<Specialty>> =
        call("GET", "/api/v1/specialties", token)

    override suspend fun professionals(token: SecretString): SchedulingResult<List<Professional>> =
        call("GET", "/api/v1/professionals", token)

    override suspend fun clinics(token: SecretString): SchedulingResult<List<Clinic>> =
        call("GET", "/api/v1/clinics", token)

    override suspend fun availability(
        token: SecretString,
        specialtyId: Int,
        professionalId: Int?,
    ): SchedulingResult<List<ClinicAvailability>> {
        val query = buildString {
            append("?specialtyId=").append(specialtyId)
            if (professionalId != null) {
                append("&professionalId=").append(professionalId)
            }
        }
        return call("GET", "/api/v1/availability$query", token)
    }

    override suspend fun book(token: SecretString, request: CreateAppointmentRequest): SchedulingResult<Appointment> =
        call("POST", "/api/v1/appointments", token, json.encodeToString(request))

    override suspend fun list(token: SecretString): SchedulingResult<List<Appointment>> =
        call("GET", "/api/v1/appointments", token)

    override suspend fun cancel(token: SecretString, appointmentId: Int): SchedulingResult<Appointment> =
        call("POST", "/api/v1/appointments/$appointmentId/cancel", token, "{}")

    private suspend inline fun <reified T> call(
        method: String,
        path: String,
        token: SecretString?,
        body: String? = null,
    ): SchedulingResult<T> = json.callApi(method, "$baseUrl$path", token, body)
}

