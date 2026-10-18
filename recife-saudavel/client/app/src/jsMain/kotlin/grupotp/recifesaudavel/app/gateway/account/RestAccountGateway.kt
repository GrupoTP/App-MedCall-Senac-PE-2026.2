package grupotp.recifesaudavel.app.gateway.account

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.http.callApi
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
    override suspend fun profile(token: SecretString): RemoteResult<AccountProfile> =
        json.callApi("GET", "$baseUrl/api/v1/account", token)

    override suspend fun update(token: SecretString, request: UpdateProfileRequest): RemoteResult<AccountProfile> =
        json.callApi("PUT", "$baseUrl/api/v1/account", token, json.encodeToString(request))

    override suspend fun updateConsents(token: SecretString, request: UpdateConsentsRequest): RemoteResult<AccountProfile> =
        json.callApi("PUT", "$baseUrl/api/v1/account/consents", token, json.encodeToString(request))

    override suspend fun changePassword(token: SecretString, request: ChangePasswordRequest): RemoteResult<AccountProfile> =
        json.callApi("POST", "$baseUrl/api/v1/account/password", token, json.encodeToString(request))

    override suspend fun requestDeletion(token: SecretString): RemoteResult<AccountProfile> =
        json.callApi("POST", "$baseUrl/api/v1/account/deletion-request", token, "{}")

    override suspend fun signOut(token: SecretString): RemoteResult<Unit> =
        when (val result = json.callApi<JsonObject>("POST", "$baseUrl/api/v1/auth/logout", token, "{}")) {
            is RemoteResult.Success -> RemoteResult.Success(Unit)
            is RemoteResult.Failed -> result
        }
}
