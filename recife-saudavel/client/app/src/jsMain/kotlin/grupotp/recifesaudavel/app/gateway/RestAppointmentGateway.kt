package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.usecase.AppointmentGateway
import grupotp.recifesaudavel.app.usecase.SchedulingResult
import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AvailableDay
import grupotp.recifesaudavel.common.CreateAppointmentRequest
import grupotp.recifesaudavel.common.Professional
import grupotp.recifesaudavel.common.Specialty
import kotlinx.serialization.json.Json

class RestAppointmentGateway(
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AppointmentGateway {
    override suspend fun specialties(): SchedulingResult<List<Specialty>> =
        call("GET", "/api/v1/specialties", token = null)

    override suspend fun professionals(token: String): SchedulingResult<List<Professional>> =
        call("GET", "/api/v1/professionals", token)

    override suspend fun availability(
        token: String,
        specialtyId: String,
        professionalId: String?,
    ): SchedulingResult<List<AvailableDay>> {
        val query = buildString {
            append("?specialtyId=").append(encodeURIComponent(specialtyId))
            if (professionalId != null) {
                append("&professionalId=").append(encodeURIComponent(professionalId))
            }
        }
        return call("GET", "/api/v1/availability$query", token)
    }

    override suspend fun book(token: String, request: CreateAppointmentRequest): SchedulingResult<Appointment> =
        call("POST", "/api/v1/appointments", token, json.encodeToString(request))

    override suspend fun list(token: String): SchedulingResult<List<Appointment>> =
        call("GET", "/api/v1/appointments", token)

    override suspend fun cancel(token: String, appointmentId: String): SchedulingResult<Appointment> =
        call("POST", "/api/v1/appointments/${encodeURIComponent(appointmentId)}/cancel", token, "{}")

    private suspend inline fun <reified T> call(
        method: String,
        path: String,
        token: String?,
        body: String? = null,
    ): SchedulingResult<T> = json.callApi(method, "$baseUrl$path", token, body)
}

private external fun encodeURIComponent(value: String): String
