package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.usecase.AuthError
import grupotp.recifesaudavel.app.usecase.AuthGateway
import grupotp.recifesaudavel.app.usecase.AuthResult
import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.serialization.json.Json

class RestAuthGateway(
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AuthGateway {
    override suspend fun signIn(request: SignInRequest): AuthResult =
        post("/api/v1/auth/login", request)

    override suspend fun register(request: RegisterAccountRequest): AuthResult =
        post("/api/v1/auth/register", request)

    override suspend fun requestPasswordRecovery(email: String): AuthResult =
        AuthResult.Failed(AuthError.NotImplemented)

    override suspend fun profile(token: SecretString): AuthResult = send("GET", "/api/v1/auth/me", body = null, token = token)

    private suspend inline fun <reified T> post(path: String, body: T): AuthResult =
        send("POST", path, json.encodeToString(body), token = null)

    private suspend fun send(method: String, path: String, body: String?, token: SecretString?): AuthResult {
        val (status, text) = try {
            sendJson(method, "$baseUrl$path", body, token)
        } catch (_: IllegalStateException) {
            return AuthResult.Failed(AuthError.Offline)
        }
        if (status == 0.toShort()) {
            return AuthResult.Failed(AuthError.Offline)
        }
        if (status == 401.toShort() && token != null) {
            return AuthResult.Failed(AuthError.SessionExpired)
        }
        return if (status in 200..299) {
            AuthResult.Success(json.decodeFromString<AuthenticatedAccount>(text))
        } else {
            val message = runCatching { json.decodeFromString<ApiError>(text).message }
                .getOrDefault("Não foi possível concluir o pedido.")
            AuthResult.Failed(AuthError.Rejected(message))
        }
    }
}
