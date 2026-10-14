package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.usecase.AuthError
import grupotp.recifesaudavel.app.usecase.AuthGateway
import grupotp.recifesaudavel.app.usecase.AuthResult
import grupotp.recifesaudavel.common.ApiError
import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.common.SignInRequest
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

    private suspend inline fun <reified T> post(path: String, body: T): AuthResult {
        val (status, text) = try {
            sendJson("POST", "$baseUrl$path", json.encodeToString(body))
        } catch (_: IllegalStateException) {
            return AuthResult.Failed(AuthError.Offline)
        }
        if (status == 0.toShort()) {
            return AuthResult.Failed(AuthError.Offline)
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
