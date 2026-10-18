package grupotp.recifesaudavel.app.gateway.scheduling

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.http.callApi
import grupotp.recifesaudavel.app.gateway.http.jsonBody
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject

class RestAppointmentGateway(
    private val baseUrl: String,
    private val http: HttpClient,
) : AppointmentGateway {
    override suspend fun specialties(token: SecretString): RemoteResult<List<Specialty>> =
        call(HttpMethod.Get, "/api/v1/specialties", token)

    override suspend fun professionals(token: SecretString): RemoteResult<List<Professional>> =
        call(HttpMethod.Get, "/api/v1/professionals", token)

    override suspend fun clinics(token: SecretString): RemoteResult<List<Clinic>> =
        call(HttpMethod.Get, "/api/v1/clinics", token)

    override suspend fun availability(
        token: SecretString,
        specialtyId: Int,
        professionalId: Int?,
    ): RemoteResult<List<ClinicAvailability>> {
        val query = buildString {
            append("?specialtyId=").append(specialtyId)
            if (professionalId != null) {
                append("&professionalId=").append(professionalId)
            }
        }
        return call(HttpMethod.Get, "/api/v1/availability$query", token)
    }

    override suspend fun book(token: SecretString, request: CreateAppointmentRequest): RemoteResult<Appointment> =
        call(HttpMethod.Post, "/api/v1/appointments", token) { jsonBody(request) }

    override suspend fun list(token: SecretString): RemoteResult<List<Appointment>> =
        call(HttpMethod.Get, "/api/v1/appointments", token)

    override suspend fun cancel(token: SecretString, appointmentId: Int): RemoteResult<Appointment> =
        call(HttpMethod.Post, "/api/v1/appointments/$appointmentId/cancel", token) { jsonBody(JsonObject(emptyMap())) }

    private suspend inline fun <reified T> call(
        method: HttpMethod,
        path: String,
        token: SecretString?,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): RemoteResult<T> = http.callApi(method, "$baseUrl$path", token, block)
}

