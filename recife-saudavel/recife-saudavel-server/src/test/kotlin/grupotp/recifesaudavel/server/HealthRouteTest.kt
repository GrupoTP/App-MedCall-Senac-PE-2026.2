package grupotp.recifesaudavel.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {
    @Test
    fun healthzReportsApplicationUp() = testApplication {
        // Arrange
        application {
            module()
        }

        // Act
        val response = client.get("/healthz")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"application":true}""", response.bodyAsText())
    }

    @Test
    fun readyzReportsApplicationUp() = testApplication {
        // Arrange
        application {
            module()
        }

        // Act
        val response = client.get("/readyz")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"application":true}""", response.bodyAsText())
    }
}
