package grupotp.recifesaudavel.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.RegisteredClaims
import com.auth0.jwt.algorithms.Algorithm
import grupotp.recifesaudavel.common.security.SecretString
import java.time.Clock

/**
 * Signs and checks the session JWT the client sends as `Authorization: Bearer`. The token names
 * the user (`sub`) and the session (`jti`); the database only keeps the session id, so a copy of
 * it holds nothing that signs in.
 *
 * There is no `exp`: a session ends after [SessionSettings][grupotp.recifesaudavel.server.service.SessionSettings]
 * idle time or when revoked, and the session row, checked on every request, is what says so.
 */
class SessionTokens(
    private val settings: JwtSettings,
    private val clock: Clock,
) {
    private val algorithm = Algorithm.HMAC256(settings.secret.value)

    /** Accepts only tokens signed here, for this audience, naming a user and a session. */
    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(settings.issuer)
        .withAudience(settings.audience)
        .withClaimPresence(RegisteredClaims.SUBJECT)
        .withClaimPresence(RegisteredClaims.JWT_ID)
        .build()

    fun sign(userId: String, sessionId: String): SecretString = SecretString(
        JWT.create()
            .withIssuer(settings.issuer)
            .withAudience(settings.audience)
            .withSubject(userId)
            .withJWTId(sessionId)
            .withIssuedAt(clock.instant())
            .sign(algorithm),
    )
}
