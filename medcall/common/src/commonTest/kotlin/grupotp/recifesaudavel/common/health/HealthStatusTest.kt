package grupotp.recifesaudavel.common.health

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthStatusTest {
    @Test
    fun serializesAndDeserializesHealthStatus() {
        // Arrange
        val status = HealthStatus(
            service = "Recife Saudável",
            status = "ok",
        )

        // Act
        val encoded = Json.encodeToString(status)
        val decoded = Json.decodeFromString<HealthStatus>(encoded)

        // Assert
        assertEquals(status, decoded)
        assertEquals("Recife Saudável", decoded.service)
        assertEquals("ok", decoded.status)
    }
}
