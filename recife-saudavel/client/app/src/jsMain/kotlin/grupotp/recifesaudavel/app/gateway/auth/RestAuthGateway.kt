package grupotp.recifesaudavel.app.gateway.auth

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.auth.AuthError
import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.app.gateway.http.callApi
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod

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
        send(HttpMethod.Post, path, token = null) { setBody(body) }

    private suspend fun send(
        method: HttpMethod,
        path: String,
        token: SecretString?,
        block: HttpRequestBuilder.() -> Unit = {},
    ): AuthResult =
        when (val result = http.callApi<AuthenticatedAccount>(method, "$baseUrl$path", token, block)) {
            is RemoteResult.Success -> AuthResult.Success(result.value)
            is RemoteResult.Failed -> AuthResult.Failed(
                when (val error = result.error) {
                    RemoteError.Offline -> AuthError.Offline
                    RemoteError.SessionExpired -> AuthError.SessionExpired
                    is RemoteError.Rejected -> AuthError.Rejected(error.message)
                },
            )
        }
}
