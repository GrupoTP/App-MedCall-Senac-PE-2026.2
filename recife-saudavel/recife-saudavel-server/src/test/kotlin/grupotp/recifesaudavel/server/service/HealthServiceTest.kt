package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.HealthStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthServiceTest {
    @Test
    fun applicationIsUpWhenSharedStatusIsOk() {
        // Arrange
        val service = HealthService()
        val expected = HealthStatus(
            service = "Recife Saudável",
            status = "ok",
        )

        // Act
        val status = service.healthStatus()
        val up = service.isApplicationUp()

        // Assert
        assertEquals(expected, status)
        assertTrue(up)
    }
}
