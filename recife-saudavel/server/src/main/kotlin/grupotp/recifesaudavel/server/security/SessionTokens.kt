package grupotp.recifesaudavel.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.RegisteredClaims
import com.auth0.jwt.algorithms.Algorithm
import grupotp.recifesaudavel.common.security.SecretString
import grupotp.recifesaudavel.server.service.OpenedSession
import kotlin.time.toJavaInstant

/**
 * Signs and checks the session JWT the client sends as `Authorization: Bearer`. The token names
 * the user (`sub`) and the session (`jti`, with its creation time as `iat`); the database only
 * keeps the session row, so a copy of it holds nothing that signs in.
 *
 * There is no `exp`: a session ends after [SessionSettings][grupotp.recifesaudavel.server.service.SessionSettings]
 * idle time or when revoked, and the session row, checked on every request, is what says so.
 */
class SessionTokens(
    private val settings: JwtSettings,
) {
    private val algorithm = Algorithm.HMAC256(settings.secret.value)

    /** Accepts only tokens signed here, for this audience, naming a user and a session. */
    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(settings.issuer)
        .withAudience(settings.audience)
        .withClaimPresence(RegisteredClaims.SUBJECT)
        .withClaimPresence(RegisteredClaims.JWT_ID)
        .withClaimPresence(RegisteredClaims.ISSUED_AT)
        .build()

    fun sign(userId: Int, session: OpenedSession): SecretString = SecretString(
        JWT.create()
            .withIssuer(settings.issuer)
            .withAudience(settings.audience)
            .withSubject(userId.toString())
            .withJWTId(session.id.toString())
            .withIssuedAt(session.createdAt.toJavaInstant())
            .sign(algorithm),
    )
}
