package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.usecase.SchedulingError
import grupotp.recifesaudavel.app.usecase.SchedulingResult
import grupotp.recifesaudavel.common.ApiError
import js.reflect.unsafeCast
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import web.events.EventHandler
import web.events.ProgressEvent
import web.http.RequestMethod
import web.xhr.XMLHttpRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val UNAUTHORIZED: Short = 401

/**
 * Sends a JSON request with XMLHttpRequest, which both react-native-web and Hermes provide.
 * Returns status and body. Status 0 or an [IllegalStateException] mean the server was not reached.
 */
internal suspend fun sendJson(
    method: String,
    url: String,
    body: String? = null,
    bearerToken: String? = null,
): Pair<Short, String> = suspendCancellableCoroutine { continuation ->
    val request = XMLHttpRequest()
    request.open(unsafeCast<RequestMethod>(method), url, true)
    request.setRequestHeader("Accept", "application/json")
    if (body != null) {
        request.setRequestHeader("Content-Type", "application/json")
    }
    if (!bearerToken.isNullOrBlank()) {
        request.setRequestHeader("Authorization", "Bearer $bearerToken")
    }
    val onLoad: () -> Unit = {
        continuation.resume(request.status to request.responseText)
    }
    val onError: () -> Unit = {
        continuation.resumeWithException(IllegalStateException("Falha de rede ao chamar $url"))
    }
    request.onload = unsafeCast<EventHandler<ProgressEvent, XMLHttpRequest, XMLHttpRequest>>(onLoad)
    request.onerror = unsafeCast<EventHandler<ProgressEvent, XMLHttpRequest, XMLHttpRequest>>(onError)
    if (body == null) {
        request.send()
    } else {
        request.send(body)
    }
}

/**
 * Calls an API route that needs the session token and decodes the answer: 401 means the session
 * expired, status 0 or a network failure mean offline, and any other error carries the server message.
 */
internal suspend inline fun <reified T> Json.callApi(
    method: String,
    url: String,
    token: String?,
    body: String? = null,
): SchedulingResult<T> {
    val (status, text) = try {
        sendJson(method, url, body, token)
    } catch (_: IllegalStateException) {
        return SchedulingResult.Failed(SchedulingError.Offline)
    }
    return when {
        status == 0.toShort() -> SchedulingResult.Failed(SchedulingError.Offline)
        status == UNAUTHORIZED -> SchedulingResult.Failed(SchedulingError.SessionExpired)
        status in 200..299 -> SchedulingResult.Success(decodeFromString<T>(text))
        else -> {
            val message = runCatching { decodeFromString<ApiError>(text).message }
                .getOrDefault("Não foi possível concluir o pedido.")
            SchedulingResult.Failed(SchedulingError.Rejected(message))
        }
    }
}
