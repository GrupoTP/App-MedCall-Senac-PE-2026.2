package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.usecase.AppointmentGateway
import grupotp.recifesaudavel.app.usecase.SchedulingError
import grupotp.recifesaudavel.app.usecase.SchedulingResult
import grupotp.recifesaudavel.common.ApiError
import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AvailableDay
import grupotp.recifesaudavel.common.CreateAppointmentRequest
import grupotp.recifesaudavel.common.Professional
import grupotp.recifesaudavel.common.Specialty
import kotlinx.serialization.json.Json

private const val UNAUTHORIZED: Short = 401

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

    private suspend inline fun <reified T> call(
        method: String,
        path: String,
        token: String?,
        body: String? = null,
    ): SchedulingResult<T> {
        val (status, text) = try {
            sendJson(method, "$baseUrl$path", body, token)
        } catch (_: IllegalStateException) {
            return SchedulingResult.Failed(SchedulingError.Offline)
        }
        return when {
            status == 0.toShort() -> SchedulingResult.Failed(SchedulingError.Offline)
            status == UNAUTHORIZED -> SchedulingResult.Failed(SchedulingError.SessionExpired)
            status in 200..299 -> SchedulingResult.Success(json.decodeFromString<T>(text))
            else -> {
                val message = runCatching { json.decodeFromString<ApiError>(text).message }
                    .getOrDefault("Não foi possível concluir o pedido.")
                SchedulingResult.Failed(SchedulingError.Rejected(message))
            }
        }
    }
}

private external fun encodeURIComponent(value: String): String
