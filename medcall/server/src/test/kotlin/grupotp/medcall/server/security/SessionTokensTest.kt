package grupotp.recifesaudavel.server.security

import com.auth0.jwt.exceptions.JWTVerificationException
import grupotp.recifesaudavel.common.security.SecretString
import grupotp.recifesaudavel.server.domain.OpenedSession
import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class SessionTokensTest {
    private val settings = JwtSettings(
        secret = SecretString("a".repeat(JwtSettings.MIN_SECRET_BYTES)),
        issuer = "recife-saudavel-server",
        audience = "recife-saudavel-app",
        realm = "recife-saudavel",
    )
    private val tokens = SessionTokens(settings)
    private val session = OpenedSession(id = 42, createdAt = Instant.parse("2026-10-12T13:00:00Z"))

    @Test
    fun aSignedTokenIsVerifiedWithItsUserAndSession() {
        // Act
        val decoded = tokens.verifier.verify(tokens.sign(userId = 7, session = session).value)

        // Assert
        assertEquals("7", decoded.subject)
        assertEquals("42", decoded.id)
        assertEquals(session.createdAt.toJavaInstant(), decoded.issuedAtAsInstant)
    }

    @Test
    fun tokensOfAnotherKeyOrAudienceAreRefused() {
        // Arrange
        val otherKey = SessionTokens(settings.copy(secret = SecretString("b".repeat(JwtSettings.MIN_SECRET_BYTES))))
        val otherAudience = SessionTokens(settings.copy(audience = "outro-app"))

        // Act + Assert
        assertFailsWith<JWTVerificationException> { tokens.verifier.verify(otherKey.sign(7, session).value) }
        assertFailsWith<JWTVerificationException> { tokens.verifier.verify(otherAudience.sign(7, session).value) }
    }

    @Test
    fun aShortSecretIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            settings.copy(secret = SecretString("curta"))
        }
    }

    @Test
    fun theServerDoesNotStartWithoutASecret() {
        // Arrange
        val config = MapApplicationConfig(
            "jwt.secret" to "",
            "jwt.issuer" to "recife-saudavel-server",
            "jwt.audience" to "recife-saudavel-app",
            "jwt.realm" to "recife-saudavel",
        )

        // Act + Assert
        assertFailsWith<IllegalStateException> { JwtSettings.from(config) }
    }
}
