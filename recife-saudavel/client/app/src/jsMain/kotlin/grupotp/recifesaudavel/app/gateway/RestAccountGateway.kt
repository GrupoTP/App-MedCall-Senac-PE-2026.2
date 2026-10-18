package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.domain.account.AccountGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class RestAccountGateway(
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AccountGateway {
    override suspend fun profile(token: SecretString): SchedulingResult<AccountProfile> =
        json.callApi("GET", "$baseUrl/api/v1/account", token)

    override suspend fun update(token: SecretString, request: UpdateProfileRequest): SchedulingResult<AccountProfile> =
        json.callApi("PUT", "$baseUrl/api/v1/account", token, json.encodeToString(request))

    override suspend fun updateConsents(token: SecretString, request: UpdateConsentsRequest): SchedulingResult<AccountProfile> =
        json.callApi("PUT", "$baseUrl/api/v1/account/consents", token, json.encodeToString(request))

    override suspend fun changePassword(token: SecretString, request: ChangePasswordRequest): SchedulingResult<AccountProfile> =
        json.callApi("POST", "$baseUrl/api/v1/account/password", token, json.encodeToString(request))

    override suspend fun requestDeletion(token: SecretString): SchedulingResult<AccountProfile> =
        json.callApi("POST", "$baseUrl/api/v1/account/deletion-request", token, "{}")

    override suspend fun signOut(token: SecretString): SchedulingResult<Unit> =
        when (val result = json.callApi<JsonObject>("POST", "$baseUrl/api/v1/auth/logout", token, "{}")) {
            is SchedulingResult.Success -> SchedulingResult.Success(Unit)
            is SchedulingResult.Failed -> result
        }
}
