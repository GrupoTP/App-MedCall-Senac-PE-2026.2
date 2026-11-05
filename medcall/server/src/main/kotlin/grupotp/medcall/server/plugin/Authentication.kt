package grupotp.recifesaudavel.server.plugin

import grupotp.recifesaudavel.server.controller.respondSessionExpired
import grupotp.recifesaudavel.server.security.JwtSettings
import grupotp.recifesaudavel.server.security.SessionPrincipal
import grupotp.recifesaudavel.server.security.SessionTokens
import grupotp.recifesaudavel.server.service.SessionService
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.header
import org.koin.ktor.ext.inject
import kotlin.time.toKotlinInstant

/** Provider of the session JWT. Routes of a signed-in person go under [authenticatedRoute]. */
const val SESSION_AUTH = "session"

fun Application.configureAuthentication() {
    val settings by inject<JwtSettings>()
    val tokens by inject<SessionTokens>()
    val sessions by inject<SessionService>()

    install(Authentication) {
        jwt(SESSION_AUTH) {
            realm = settings.realm
            verifier(tokens.verifier)
            // The signature proves the token came from this server; the database says whether its
            // session is still open, so logout and revocation work at once.
            validate { credential ->
                val userId = credential.payload.subject.toIntOrNull()
                val sessionId = credential.payload.id.toIntOrNull()
                val issuedAt = credential.payload.issuedAtAsInstant.toKotlinInstant()
                if (userId != null && sessionId != null && sessions.userIdFor(sessionId, issuedAt) == userId) {
                    SessionPrincipal(userId, sessionId)
                } else {
                    null
                }
            }
            challenge { scheme, realm ->
                val header = HttpAuthHeader.Parameterized(scheme, mapOf(HttpAuthHeader.Parameters.Realm to realm))
                call.response.header(HttpHeaders.WWWAuthenticate, header.render())
                call.respondSessionExpired()
            }
        }
    }
}
