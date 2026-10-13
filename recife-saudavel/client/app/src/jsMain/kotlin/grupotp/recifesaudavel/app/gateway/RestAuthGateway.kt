package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.usecase.AuthError
import grupotp.recifesaudavel.app.usecase.AuthGateway
import grupotp.recifesaudavel.app.usecase.AuthResult
import grupotp.recifesaudavel.common.ApiError
import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.common.SignInRequest
import js.reflect.unsafeCast
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import web.events.EventHandler
import web.events.ProgressEvent
import web.http.RequestMethod
import web.xhr.XMLHttpRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val postMethod: RequestMethod = unsafeCast("POST")

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
            postJson("$baseUrl$path", json.encodeToString(body))
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

private suspend fun postJson(url: String, body: String): Pair<Short, String> = suspendCancellableCoroutine { continuation ->
    val request = XMLHttpRequest()
    request.open(postMethod, url, true)
    request.setRequestHeader("Accept", "application/json")
    request.setRequestHeader("Content-Type", "application/json")
    val onLoad: () -> Unit = {
        continuation.resume(request.status to request.responseText)
    }
    val onError: () -> Unit = {
        continuation.resumeWithException(IllegalStateException("Falha de rede ao chamar $url"))
    }
    request.onload = unsafeCast<EventHandler<ProgressEvent, XMLHttpRequest, XMLHttpRequest>>(onLoad)
    request.onerror = unsafeCast<EventHandler<ProgressEvent, XMLHttpRequest, XMLHttpRequest>>(onError)
    request.send(body)
}
