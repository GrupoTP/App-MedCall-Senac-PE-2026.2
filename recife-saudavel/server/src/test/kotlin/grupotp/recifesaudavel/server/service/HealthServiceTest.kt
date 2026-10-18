package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.HealthStatus
import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthServiceTest {
    @Test
    fun applicationIsUpWhenSharedStatusIsOk() {
        // Arrange
        val service = HealthService(connect("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1"))
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

    @Test
    fun databaseIsReachableWhenItAnswers() {
        // Arrange
        val database = connect("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1")
        val service = HealthService(database)

        // Act
        val reachable = service.isDatabaseReachable()

        // Assert
        assertTrue(reachable)
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun databaseIsUnreachableWhenTheConnectionFails() {
        // Arrange: IFEXISTS makes H2 refuse to create the missing file, like a database that is down.
        val missing = Files.createTempDirectory("health").resolve("missing").toString().replace('\\', '/')
        val database = connect("jdbc:h2:file:$missing;IFEXISTS=TRUE")
        val service = HealthService(database)

        // Act
        val reachable = service.isDatabaseReachable()

        // Assert
        assertFalse(reachable)
        TransactionManager.closeAndUnregister(database)
    }

    private fun connect(jdbcUrl: String): Database = DatabaseConnector().connect(
        DatabaseSettings(jdbcUrl = jdbcUrl, driver = "org.h2.Driver", user = "sa", password = ""),
    )
}
