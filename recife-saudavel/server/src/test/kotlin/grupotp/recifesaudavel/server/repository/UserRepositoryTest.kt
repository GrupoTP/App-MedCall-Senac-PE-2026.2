package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.common.UpdateProfileRequest
import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import grupotp.recifesaudavel.server.database.UsersTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The unique e-mail index is the last word when two requests pass the service checks at once. */
class UserRepositoryTest {
    private lateinit var database: Database
    private lateinit var users: UserRepository

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
        transaction(database) { SchemaUtils.create(UsersTable) }
        users = UserRepository(database)
    }

    @AfterTest
    fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun insertingATakenEmailReturnsNullInsteadOfThrowing() {
        // Arrange
        val first = users.insert(UUID.randomUUID().toString(), registration("maria@email.com"), "hash")

        // Act
        val second = users.insert(UUID.randomUUID().toString(), registration("maria@email.com"), "hash")

        // Assert
        assertNotNull(first)
        assertNull(second)
    }

    @Test
    fun movingToATakenEmailReturnsFalseInsteadOfThrowing() {
        // Arrange
        users.insert(UUID.randomUUID().toString(), registration("maria@email.com"), "hash")
        val ana = users.insert(UUID.randomUUID().toString(), registration("ana@email.com"), "hash")!!

        // Act
        val updated = users.updateProfile(
            ana.id,
            UpdateProfileRequest(
                fullName = "Ana",
                socialName = "",
                email = "maria@email.com",
                primaryPhone = "81999999999",
                primaryPhoneWhatsapp = false,
                secondaryPhone = "",
                secondaryPhoneWhatsapp = false,
                emergencyContactName = "",
                emergencyContactPhone = "",
            ),
        )

        // Assert
        assertFalse(updated)
    }

    private fun registration(email: String) = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = "",
        email = email,
        password = "segredo",
        primaryPhone = "(81) 99999-9999",
        primaryPhoneWhatsapp = true,
        secondaryPhone = "",
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = "",
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
}
