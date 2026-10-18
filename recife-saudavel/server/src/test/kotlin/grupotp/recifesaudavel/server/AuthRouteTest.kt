package grupotp.recifesaudavel.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthRouteTest {
    @Test
    fun registerThenLoginWithJson() = testApplication {
        // Arrange
        environment {
            config = ApplicationConfig("application.yaml")
        }
        val email = "maria.${System.nanoTime()}@email.com"
        val password = "segredo"
        val registration = registration(email, password)

        // Act
        val created = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(registration))
        }
        val loggedIn = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(SignInRequest(email = email, password = SecretString(password))))
        }

        // Assert
        assertEquals(HttpStatusCode.Created, created.status)
        val account = Json.decodeFromString<AuthenticatedAccount>(created.bodyAsText())
        assertEquals(email, account.email)
        assertEquals(HttpStatusCode.OK, loggedIn.status)
        val signedIn = Json.decodeFromString<AuthenticatedAccount>(loggedIn.bodyAsText())
        assertEquals(account.copy(sessionToken = SecretString.EMPTY), signedIn.copy(sessionToken = SecretString.EMPTY))
        assertTrue(account.sessionToken.value.isNotBlank())
        assertNotEquals(account.sessionToken, signedIn.sessionToken)
    }

    @Test
    fun meReturnsTheStoredProfileForAValidSessionOnly() = testApplication {
        // Arrange
        environment {
            config = ApplicationConfig("application.yaml")
        }
        val email = "me.${System.nanoTime()}@email.com"
        val created = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(registration(email, "segredo")))
        }
        val account = Json.decodeFromString<AuthenticatedAccount>(created.bodyAsText())

        // Act
        val me = client.get("/api/v1/auth/me") { bearerAuth(account.sessionToken.value) }
        val forged = client.get("/api/v1/auth/me") { bearerAuth("token-inventado") }
        val resigned = client.get("/api/v1/auth/me") { bearerAuth(signedWithAnotherKey(account.sessionToken.value)) }
        val anonymous = client.get("/api/v1/auth/me")

        // Assert
        assertEquals(HttpStatusCode.OK, me.status)
        val profile = Json.decodeFromString<AuthenticatedAccount>(me.bodyAsText())
        assertEquals(account.copy(sessionToken = SecretString.EMPTY), profile.copy(sessionToken = SecretString.EMPTY))
        assertTrue(!profile.phone.isEmpty())
        assertEquals(HttpStatusCode.Unauthorized, forged.status)
        assertEquals(HttpStatusCode.Unauthorized, resigned.status)
        assertEquals("Bearer realm=recife-saudavel", resigned.headers[HttpHeaders.WWWAuthenticate])
        assertEquals(HttpStatusCode.Unauthorized, anonymous.status)
        assertEquals("Sua sessão expirou. Entre de novo para continuar.", Json.decodeFromString<ApiError>(resigned.bodyAsText()).message)
    }

    @Test
    fun theTokenIsAJwtThatNamesTheUserAndTheSession() = testApplication {
        // Arrange
        environment {
            config = ApplicationConfig("application.yaml")
        }
        val email = "jwt.${System.nanoTime()}@email.com"

        // Act
        val created = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(registration(email, "segredo")))
        }

        // Assert
        val account = Json.decodeFromString<AuthenticatedAccount>(created.bodyAsText())
        val token = JWT.decode(account.sessionToken.value)
        assertEquals("HS256", token.algorithm)
        assertEquals(account.id.toString(), token.subject)
        assertTrue(token.id.isNotBlank())
        assertEquals(setOf("sub", "jti", "iss", "aud", "iat"), token.claims.keys)
    }

    @Test
    fun loginRejectsUnknownPassword() = testApplication {
        // Arrange
        environment {
            config = ApplicationConfig("application.yaml")
        }
        val email = "ana.${System.nanoTime()}@email.com"
        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(registration(email, "segredo")))
        }

        // Act
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(SignInRequest(email = email, password = SecretString("errada1"))))
        }

        // Assert
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(
            "E-mail ou senha incorretos.",
            Json.decodeFromString<ApiError>(response.bodyAsText()).message,
        )
    }

    /** Same claims as [token], signed with a key the server does not know. */
    private fun signedWithAnotherKey(token: String): String {
        val decoded = JWT.decode(token)
        return JWT.create()
            .withIssuer(decoded.issuer)
            .withAudience(*decoded.audience.toTypedArray())
            .withSubject(decoded.subject)
            .withJWTId(decoded.id)
            .withIssuedAt(decoded.issuedAtAsInstant)
            .sign(Algorithm.HMAC256("uma-chave-que-o-servidor-nao-conhece"))
    }

    private fun registration(email: String, password: String) = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = "Graça",
        email = email,
        password = SecretString(password),
        primaryPhone = Phone.parse("(81) 99999-9999"),
        primaryPhoneWhatsapp = true,
        secondaryPhone = Phone.parse(""),
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "Ana Silva",
        emergencyContactPhone = Phone.parse("(81) 98888-7777"),
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
}
