package grupotp.medcall.server.plugin

import grupotp.medcall.server.controller.respondSessionExpired
import grupotp.medcall.server.security.JwtSettings
import grupotp.medcall.server.security.SessionPrincipal
import grupotp.medcall.server.security.SessionTokens
import grupotp.medcall.server.security.StaffTokens
import grupotp.medcall.server.service.StaffService
import grupotp.medcall.server.service.StaffSessionService
import grupotp.medcall.server.service.SessionService
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.auth.jwt.jwt
import grupotp.medcall.server.integration.IntegrationService
import io.ktor.server.response.header
import org.koin.ktor.ext.inject
import kotlin.time.toKotlinInstant

/** Provider of the session JWT. Routes of a signed-in person go under [authenticatedRoute]. */
const val SESSION_AUTH = "session"

/** Provider of the staff JWT (clinic portal and MedCall panel). Routes go under [staffRoute]. */
const val STAFF_AUTH = "staff"

/** Provider of the clinic systems' API keys on the public API (`Authorization: Bearer mc_live_...`). */
const val API_KEY_AUTH = "api-key"

fun Application.configureAuthentication() {
    val settings by inject<JwtSettings>()
    val tokens by inject<SessionTokens>()
    val sessions by inject<SessionService>()
    val staffTokens by inject<StaffTokens>()
    val staffSessions by inject<StaffSessionService>()
    val staff by inject<StaffService>()
    val integration by inject<IntegrationService>()

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
        jwt(STAFF_AUTH) {
            realm = settings.realm
            verifier(staffTokens.verifier)
            validate { credential ->
                val staffId = credential.payload.subject.toIntOrNull()
                val sessionId = credential.payload.id.toIntOrNull()
                val issuedAt = credential.payload.issuedAtAsInstant.toKotlinInstant()
                if (staffId != null && sessionId != null && staffSessions.userIdFor(sessionId, issuedAt) == staffId) {
                    staff.principalOf(staffId, sessionId)
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
        bearer(API_KEY_AUTH) {
            realm = "medcall-public-api"
            authenticate { credential -> integration.authenticate(credential.token) }
        }
    }
}
