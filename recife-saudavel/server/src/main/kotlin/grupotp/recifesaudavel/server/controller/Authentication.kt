package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.ApiError
import grupotp.recifesaudavel.server.service.SessionService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

private const val BEARER_PREFIX = "Bearer "

internal const val SESSION_EXPIRED_MESSAGE = "Sua sessão expirou. Entre de novo para continuar."

/** Token of the `Authorization: Bearer` header, or null when there is none. */
internal fun ApplicationCall.bearerToken(): String? {
    val header = request.headers[HttpHeaders.Authorization].orEmpty()
    if (!header.startsWith(BEARER_PREFIX)) {
        return null
    }
    return header.removePrefix(BEARER_PREFIX).trim().takeIf { it.isNotEmpty() }
}

/** User id of the bearer token, or null after answering 401. */
internal suspend fun ApplicationCall.authenticatedUserId(sessions: SessionService): String? {
    val userId = bearerToken()?.let { sessions.userIdFor(it) }
    if (userId == null) {
        respondSessionExpired()
    }
    return userId
}

internal suspend fun ApplicationCall.respondSessionExpired() {
    respond(HttpStatusCode.Unauthorized, ApiError(SESSION_EXPIRED_MESSAGE))
}
