package grupotp.recifesaudavel.app.gateway.http

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

private const val REQUEST_TIMEOUT_MILLIS = 15_000L

/**
 * HTTP client of the Rest gateways. The Js engine sends requests with fetch, which both
 * react-native-web and Hermes provide. Tests pass a MockEngine instead.
 */
fun apiHttpClient(engine: HttpClientEngine = Js.create()): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
    defaultRequest {
        accept(ContentType.Application.Json)
    }
}

/**
 * Sends a JSON request. Returns null when the server was not reached: network failure or timeout.
 */
internal suspend fun HttpClient.sendJson(
    method: HttpMethod,
    url: String,
    token: SecretString?,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse? = try {
    request(url) {
        this.method = method
        if (token != null && !token.isBlank()) {
            bearerAuth(token.value)
        }
        block()
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

/** Sets [body] as the JSON request body, encoded by ContentNegotiation. */
internal inline fun <reified B> HttpRequestBuilder.jsonBody(body: B) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

/** Message of an error answer, from its [ApiError] body when there is one. */
internal suspend fun HttpResponse.errorMessage(): String =
    runCatching { body<ApiError>().message }.getOrDefault("Não foi possível concluir o pedido.")

/**
 * Calls an API route that needs the session token and decodes the answer: 401 means the session
 * expired, a network failure or timeout means offline, and any other error carries the server message.
 */
internal suspend inline fun <reified T> HttpClient.callApi(
    method: HttpMethod,
    url: String,
    token: SecretString?,
    noinline block: HttpRequestBuilder.() -> Unit = {},
): RemoteResult<T> {
    val response = sendJson(method, url, token, block) ?: return RemoteResult.Failed(RemoteError.Offline)
    return when {
        response.status == HttpStatusCode.Unauthorized -> RemoteResult.Failed(RemoteError.SessionExpired)
        response.status.isSuccess() -> RemoteResult.Success(response.body<T>())
        else -> RemoteResult.Failed(RemoteError.Rejected(response.errorMessage()))
    }
}
