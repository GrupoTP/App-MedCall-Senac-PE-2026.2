package grupotp.recifesaudavel.app.gateway.auth

import grupotp.recifesaudavel.app.domain.auth.AuthError
import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.app.gateway.http.errorMessage
import grupotp.recifesaudavel.app.gateway.http.jsonBody
import grupotp.recifesaudavel.app.gateway.http.sendJson
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

class RestAuthGateway(
    private val baseUrl: String,
    private val http: HttpClient,
) : AuthGateway {
    override suspend fun signIn(request: SignInRequest): AuthResult =
        post("/api/v1/auth/login", request)

    override suspend fun register(request: RegisterAccountRequest): AuthResult =
        post("/api/v1/auth/register", request)

    override suspend fun requestPasswordRecovery(email: String): AuthResult =
        AuthResult.Failed(AuthError.NotImplemented)

    override suspend fun profile(token: SecretString): AuthResult = send(HttpMethod.Get, "/api/v1/auth/me", token)

    private suspend inline fun <reified T> post(path: String, body: T): AuthResult =
        send(HttpMethod.Post, path, token = null) { jsonBody(body) }

    private suspend fun send(
        method: HttpMethod,
        path: String,
        token: SecretString?,
        block: HttpRequestBuilder.() -> Unit = {},
    ): AuthResult {
        val response = http.sendJson(method, "$baseUrl$path", token, block)
            ?: return AuthResult.Failed(AuthError.Offline)
        if (response.status == HttpStatusCode.Unauthorized && token != null) {
            return AuthResult.Failed(AuthError.SessionExpired)
        }
        return if (response.status.isSuccess()) {
            AuthResult.Success(response.body<AuthenticatedAccount>())
        } else {
            AuthResult.Failed(AuthError.Rejected(response.errorMessage()))
        }
    }
}
