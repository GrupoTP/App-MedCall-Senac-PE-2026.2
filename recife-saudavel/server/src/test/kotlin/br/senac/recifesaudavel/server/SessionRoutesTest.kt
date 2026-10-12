package br.senac.recifesaudavel.server

import br.senac.recifesaudavel.common.ErrorResponse
import br.senac.recifesaudavel.common.LoginRequest
import br.senac.recifesaudavel.common.SessionResponse
import br.senac.recifesaudavel.common.appJson
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionRoutesTest {
    @Test
    fun loginWithMockUserReturnsSession() = testApplication {
        // Arrange
        application { module() }
        val request = LoginRequest(email = "  ${MockUser.EMAIL}  ", password = MockUser.PASSWORD)

        // Act
        val response = client.post("/api/sessions") {
            contentType(ContentType.Application.Json)
            setBody(appJson.encodeToString(request))
        }

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val session = appJson.decodeFromString<SessionResponse>(response.bodyAsText())
        assertEquals(MockUser.NAME, session.user.name)
        assertTrue(session.token.isNotBlank())
    }

    @Test
    fun loginWithWrongPasswordReturnsUnauthorized() = testApplication {
        // Arrange
        application { module() }
        val request = LoginRequest(email = MockUser.EMAIL, password = "errada")

        // Act
        val response = client.post("/api/sessions") {
            contentType(ContentType.Application.Json)
            setBody(appJson.encodeToString(request))
        }

        // Assert
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val error = appJson.decodeFromString<ErrorResponse>(response.bodyAsText())
        assertEquals("E-mail ou senha inválidos.", error.message)
    }

    @Test
    fun loginWithBlankEmailReturnsValidationMessage() = testApplication {
        // Arrange
        application { module() }
        val request = LoginRequest(email = " ", password = MockUser.PASSWORD)

        // Act
        val response = client.post("/api/sessions") {
            contentType(ContentType.Application.Json)
            setBody(appJson.encodeToString(request))
        }

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = appJson.decodeFromString<ErrorResponse>(response.bodyAsText())
        assertEquals("Informe o e-mail.", error.message)
    }

    @Test
    fun logoutInvalidatesTheSession() = testApplication {
        // Arrange
        application { module() }
        val loginResponse = client.post("/api/sessions") {
            contentType(ContentType.Application.Json)
            setBody(appJson.encodeToString(LoginRequest(MockUser.EMAIL, MockUser.PASSWORD)))
        }
        val token = appJson.decodeFromString<SessionResponse>(loginResponse.bodyAsText()).token

        // Act
        val logoutResponse = client.delete("/api/sessions/current") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val currentResponse = client.get("/api/sessions/current") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        // Assert
        assertEquals(HttpStatusCode.NoContent, logoutResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, currentResponse.status)
    }
}
