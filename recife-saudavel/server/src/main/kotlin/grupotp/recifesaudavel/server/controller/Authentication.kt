package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.ApiError
import grupotp.recifesaudavel.server.service.SessionService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

private const val BEARER_PREFIX = "Bearer "

/** User id of the bearer token, or null after answering 401. */
internal suspend fun ApplicationCall.authenticatedUserId(sessions: SessionService): String? {
    val header = request.headers[HttpHeaders.Authorization].orEmpty()
    val userId = if (header.startsWith(BEARER_PREFIX)) {
        sessions.userIdFor(header.removePrefix(BEARER_PREFIX).trim())
    } else {
        null
    }
    if (userId == null) {
        respond(HttpStatusCode.Unauthorized, ApiError("Sua sessão expirou. Entre de novo para continuar."))
    }
    return userId
}
