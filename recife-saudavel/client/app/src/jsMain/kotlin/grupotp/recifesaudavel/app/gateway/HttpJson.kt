package grupotp.recifesaudavel.app.gateway

import js.reflect.unsafeCast
import kotlinx.coroutines.suspendCancellableCoroutine
import web.events.EventHandler
import web.events.ProgressEvent
import web.http.RequestMethod
import web.xhr.XMLHttpRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
