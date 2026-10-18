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
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendIfNameAbsent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

private const val REQUEST_TIMEOUT_MILLIS = 15_000L

/**
 * HTTP client of the Rest gateways. The Js engine sends requests with fetch, which both
 * react-native-web and Hermes provide. Tests pass a MockEngine instead.
 * Bodies go out as JSON: a request only needs `setBody`.
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
        headers.appendIfNameAbsent(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}

/** Message of an error answer, from its [ApiError] body when there is one. */
internal suspend fun HttpResponse.errorMessage(): String =
    runCatching { body<ApiError>().message }.getOrDefault("Não foi possível concluir o pedido.")

/**
 * Calls an API route and decodes the answer: a network failure or timeout means offline, 401 on a
 * request that carried [token] means the session expired, and any other error carries the server
 * message (a 401 without a token, such as a wrong password at sign-in, included).
 */
internal suspend inline fun <reified T> HttpClient.callApi(
    method: HttpMethod,
    url: String,
    token: SecretString? = null,
    noinline block: HttpRequestBuilder.() -> Unit = {},
): RemoteResult<T> {
    val response = try {
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
        return RemoteResult.Failed(RemoteError.Offline)
    }
    return when {
        response.status == HttpStatusCode.Unauthorized && HttpHeaders.Authorization in response.call.request.headers ->
            RemoteResult.Failed(RemoteError.SessionExpired)
        response.status.isSuccess() -> RemoteResult.Success(response.body<T>())
        else -> RemoteResult.Failed(RemoteError.Rejected(response.errorMessage()))
    }
}
