package grupotp.medcall.common.health

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthStatusTest {
    @Test
    fun serializesAndDeserializesHealthStatus() {
        // Arrange
        val status = HealthStatus(
            service = "MedCall",
            status = "ok",
        )

        // Act
        val encoded = Json.encodeToString(status)
        val decoded = Json.decodeFromString<HealthStatus>(encoded)

        // Assert
        assertEquals(status, decoded)
        assertEquals("MedCall", decoded.service)
        assertEquals("ok", decoded.status)
    }
}
