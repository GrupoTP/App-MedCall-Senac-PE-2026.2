package grupotp.recifesaudavel.server.controller

import grupotp.recifesaudavel.common.api.ErrorCode
import grupotp.recifesaudavel.server.security.SessionPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

/**
 * The signed-in person. Only for routes under `authenticatedRoute`, where Ktor has already
 * answered 401 to anyone without an open session.
 */
internal val ApplicationCall.signedIn: SessionPrincipal
    get() = checkNotNull(principal<SessionPrincipal>()) { "Route is not under authenticatedRoute." }

internal suspend fun ApplicationCall.respondSessionExpired() {
    respondError(HttpStatusCode.Unauthorized, ErrorCode.SESSION_EXPIRED)
}
