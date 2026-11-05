package grupotp.recifesaudavel.worker.http

import web.http.BodyInit
import web.http.Headers
import web.http.RequestMethod
import web.http.Response
import web.http.ResponseInit

/**
 * Responses the worker builds itself. Errors carry no body: the page only reads the status and
 * shows its own text.
 */
object WorkerResponses {
    private const val OK: Short = 200
    private const val NO_CONTENT: Short = 204
    private const val BAD_REQUEST: Short = 400
    private const val METHOD_NOT_ALLOWED: Short = 405
    private const val SERVICE_UNAVAILABLE: Short = 503

    /** Shown by the browser when the page was never stored and there is no connection. */
    private const val OFFLINE_TEXT =
        "Sem conexão. Abra o Recife Saudável uma vez com internet para guardar a interface neste aparelho."

    /** [body] is JSON already encoded by kotlinx.serialization. */
    fun json(body: String): Response = Response(
        BodyInit(body),
        ResponseInit(status = OK, headers = headers("Content-Type" to "application/json")),
    )

    // 204 and the errors below must not carry a body, not even an empty string, or the constructor throws.
    fun noContent(): Response = Response(null, ResponseInit(status = NO_CONTENT))

    fun badRequest(): Response = Response(null, ResponseInit(status = BAD_REQUEST))

    fun methodNotAllowed(vararg allowed: RequestMethod): Response = Response(
        null,
        ResponseInit(status = METHOD_NOT_ALLOWED, headers = headers("Allow" to allowed.joinToString())),
    )

    fun offline(): Response = Response(
        BodyInit(OFFLINE_TEXT),
        ResponseInit(
            status = SERVICE_UNAVAILABLE,
            headers = headers("Content-Type" to "text/plain; charset=utf-8"),
        ),
    )

    private fun headers(vararg entries: Pair<String, String>): Headers = Headers().apply {
        entries.forEach { (name, value) -> set(name, value) }
    }
}
