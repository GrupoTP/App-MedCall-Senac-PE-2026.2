package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.usecase.AccountGateway
import grupotp.recifesaudavel.app.usecase.SchedulingResult
import grupotp.recifesaudavel.common.AccountProfile
import grupotp.recifesaudavel.common.ChangePasswordRequest
import grupotp.recifesaudavel.common.UpdateConsentsRequest
import grupotp.recifesaudavel.common.UpdateProfileRequest
import kotlinx.serialization.json.Json

class RestAccountGateway(
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AccountGateway {
    override suspend fun profile(token: String): SchedulingResult<AccountProfile> =
        json.callApi("GET", "$baseUrl/api/v1/account", token)

    override suspend fun update(token: String, request: UpdateProfileRequest): SchedulingResult<AccountProfile> =
        json.callApi("PUT", "$baseUrl/api/v1/account", token, json.encodeToString(request))

    override suspend fun updateConsents(token: String, request: UpdateConsentsRequest): SchedulingResult<AccountProfile> =
        json.callApi("PUT", "$baseUrl/api/v1/account/consents", token, json.encodeToString(request))

    override suspend fun changePassword(token: String, request: ChangePasswordRequest): SchedulingResult<AccountProfile> =
        json.callApi("POST", "$baseUrl/api/v1/account/password", token, json.encodeToString(request))

    override suspend fun requestDeletion(token: String): SchedulingResult<AccountProfile> =
        json.callApi("POST", "$baseUrl/api/v1/account/deletion-request", token, "{}")
}
