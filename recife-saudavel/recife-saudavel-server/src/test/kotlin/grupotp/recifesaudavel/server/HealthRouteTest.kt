package grupotp.recifesaudavel.server

import grupotp.recifesaudavel.common.HealthStatus
import grupotp.recifesaudavel.common.healthStatus
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {
    @Test
    fun healthReturnsSharedStatus() = testApplication {
        // Arrange
        application {
            module()
        }
        val expected = healthStatus()

        // Act
        val response = client.get("/api/v1/health")
        val body = Json.decodeFromString<HealthStatus>(response.bodyAsText())

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(expected, body)
    }
}
