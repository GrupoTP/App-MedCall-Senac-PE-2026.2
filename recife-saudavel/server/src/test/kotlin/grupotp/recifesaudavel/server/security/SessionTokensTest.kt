package grupotp.recifesaudavel.server.security

import com.auth0.jwt.exceptions.JWTVerificationException
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.server.config.MapApplicationConfig
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SessionTokensTest {
    private val settings = JwtSettings(
        secret = SecretString("a".repeat(JwtSettings.MIN_SECRET_BYTES)),
        issuer = "recife-saudavel-server",
        audience = "recife-saudavel-app",
        realm = "recife-saudavel",
    )
    private val tokens = SessionTokens(settings, Clock.systemUTC())

    @Test
    fun aSignedTokenIsVerifiedWithItsUserAndSession() {
        // Act
        val decoded = tokens.verifier.verify(tokens.sign(userId = "maria", sessionId = "sessao-1").value)

        // Assert
        assertEquals("maria", decoded.subject)
        assertEquals("sessao-1", decoded.id)
    }

    @Test
    fun tokensOfAnotherKeyOrAudienceAreRefused() {
        // Arrange
        val otherKey = SessionTokens(settings.copy(secret = SecretString("b".repeat(JwtSettings.MIN_SECRET_BYTES))), Clock.systemUTC())
        val otherAudience = SessionTokens(settings.copy(audience = "outro-app"), Clock.systemUTC())

        // Act + Assert
        assertFailsWith<JWTVerificationException> { tokens.verifier.verify(otherKey.sign("maria", "sessao-1").value) }
        assertFailsWith<JWTVerificationException> { tokens.verifier.verify(otherAudience.sign("maria", "sessao-1").value) }
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
