package grupotp.recifesaudavel.common

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthStatusTest {
    @Test
    fun serializesAndDeserializesHealthStatus() {
        // Arrange
        val status = healthStatus()

        // Act
        val encoded = Json.encodeToString(status)
        val decoded = Json.decodeFromString<HealthStatus>(encoded)

        // Assert
        assertEquals(status, decoded)
        assertEquals("Recife Saudável", decoded.service)
        assertEquals("ok", decoded.status)
    }
}
