package grupotp.recifesaudavel.server.database

import grupotp.recifesaudavel.server.database.table.UsersTable
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LegacySchemaCheckTest {
    private lateinit var database: Database

    @BeforeTest
    fun openDatabase() {
        database = DatabaseConnector().connect(
            DatabaseSettings(
                jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = "",
            ),
        )
    }

    @AfterTest
    fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun anEmptyOrCurrentDatabaseIsAccepted() {
        // Act + Assert: no exception.
        LegacySchemaCheck.run(database)
        transaction(database) { SchemaUtils.create(UsersTable) }
        LegacySchemaCheck.run(database)
    }

    @Test
    fun aDatabaseWithTextIdsIsRefused() {
        // Arrange
        transaction(database) { SchemaUtils.create(LegacyUsersTable) }

        // Act + Assert
        assertFailsWith<IllegalStateException> { LegacySchemaCheck.run(database) }
    }

    /** The `users` table as it was with UUID text ids. */
    private object LegacyUsersTable : Table("users") {
        val id = varchar("id", 36)

        override val primaryKey = PrimaryKey(id)
    }
}
