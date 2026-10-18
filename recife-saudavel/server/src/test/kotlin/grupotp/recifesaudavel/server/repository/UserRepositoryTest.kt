package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.security.SecretString
import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import grupotp.recifesaudavel.server.database.table.UsersTable
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
import kotlin.time.Instant

private val NOW = Instant.parse("2026-10-12T13:00:00Z")

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
        val first = users.insert(registration("maria@email.com"), "hash", NOW)

        // Act
        val second = users.insert(registration("maria@email.com"), "hash", NOW)

        // Assert
        assertNotNull(first)
        assertNull(second)
    }

    @Test
    fun movingToATakenEmailReturnsFalseInsteadOfThrowing() {
        // Arrange
        users.insert(registration("maria@email.com"), "hash", NOW)
        val ana = users.insert(registration("ana@email.com"), "hash", NOW)!!

        // Act
        val updated = users.updateProfile(
            ana.id,
            UpdateProfileRequest(
                fullName = "Ana",
                socialName = "",
                email = "maria@email.com",
                primaryPhone = Phone.parse("81999999999"),
                primaryPhoneWhatsapp = false,
                secondaryPhone = Phone.parse(""),
                secondaryPhoneWhatsapp = false,
                emergencyContactName = "",
                emergencyContactPhone = Phone.parse(""),
            ),
        )

        // Assert
        assertFalse(updated)
    }

    private fun registration(email: String) = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = "",
        email = email,
        password = SecretString("segredo"),
        primaryPhone = Phone.parse("(81) 99999-9999"),
        primaryPhoneWhatsapp = true,
        secondaryPhone = Phone.parse(""),
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = Phone.parse(""),
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
}
