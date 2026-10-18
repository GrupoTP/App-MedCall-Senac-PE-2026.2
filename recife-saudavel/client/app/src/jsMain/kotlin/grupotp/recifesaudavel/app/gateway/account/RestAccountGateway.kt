package grupotp.recifesaudavel.app.gateway.account

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.http.callApi
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject

class RestAccountGateway(
    private val baseUrl: String,
    private val http: HttpClient,
) : AccountGateway {
    override suspend fun profile(token: SecretString): RemoteResult<AccountProfile> =
        http.callApi(HttpMethod.Get, "$baseUrl/api/v1/account", token)

    override suspend fun update(token: SecretString, request: UpdateProfileRequest): RemoteResult<AccountProfile> =
        http.callApi(HttpMethod.Put, "$baseUrl/api/v1/account", token) { setBody(request) }

    override suspend fun updateConsents(token: SecretString, request: UpdateConsentsRequest): RemoteResult<AccountProfile> =
        http.callApi(HttpMethod.Put, "$baseUrl/api/v1/account/consents", token) { setBody(request) }

    override suspend fun changePassword(token: SecretString, request: ChangePasswordRequest): RemoteResult<AccountProfile> =
        http.callApi(HttpMethod.Post, "$baseUrl/api/v1/account/password", token) { setBody(request) }

    override suspend fun requestDeletion(token: SecretString): RemoteResult<AccountProfile> =
        http.callApi(HttpMethod.Post, "$baseUrl/api/v1/account/deletion-request", token) { setBody(JsonObject(emptyMap())) }

    override suspend fun signOut(token: SecretString): RemoteResult<Unit> =
        when (val result = http.callApi<JsonObject>(HttpMethod.Post, "$baseUrl/api/v1/auth/logout", token) { setBody(JsonObject(emptyMap())) }) {
            is RemoteResult.Success -> RemoteResult.Success(Unit)
            is RemoteResult.Failed -> result
        }
}
