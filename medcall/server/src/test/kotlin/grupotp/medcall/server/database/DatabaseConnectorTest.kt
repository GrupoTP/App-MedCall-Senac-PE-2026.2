package grupotp.medcall.server.database

import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseConnectorTest {
    @Test
    fun fileDatabaseAcceptsAQuery() {
        // Arrange
        val folder = Files.createTempDirectory("medcall-h2")
        val databaseFile = folder.resolve("medcall")
        val settings = DatabaseSettings(
            jdbcUrl = "jdbc:h2:file:$databaseFile",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        val database = DatabaseConnector().connect(settings)

        // Act
        val value = transaction(database) {
            exec("SELECT 1") { resultSet ->
                resultSet.next()
                resultSet.getInt(1)
            }
        }

        // Assert
        assertEquals(1, value)
        assertTrue(Files.exists(folder.resolve("medcall.mv.db")))
        TransactionManager.closeAndUnregister(database)
    }
}
