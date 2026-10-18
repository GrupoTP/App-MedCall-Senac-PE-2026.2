package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import grupotp.recifesaudavel.server.database.SessionsTable
import grupotp.recifesaudavel.server.database.UsersTable
import grupotp.recifesaudavel.server.repository.SessionRepository
import grupotp.recifesaudavel.server.repository.UserRepository
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionServiceTest {
    private val clock = MovableClock(Instant.parse("2026-10-12T13:00:00Z"))

    private lateinit var database: Database
    private lateinit var sessions: SessionService
    private lateinit var maria: String

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
        transaction(database) { SchemaUtils.create(UsersTable, SessionsTable) }
        sessions = SessionService(SessionRepository(database), clock)
        maria = UserRepository(database).insert(UUID.randomUUID().toString(), registration(), "hash")!!.id
    }

    @AfterTest
    fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun aTokenStopsWorkingOnceItsLifetimeIsOver() {
        // Arrange
        val token = sessions.issue(maria)

        // Act
        clock.advance(SessionService.LIFETIME.minusMinutes(1))
        val almost = sessions.userIdFor(token)
        clock.advance(Duration.ofMinutes(1))
        val expired = sessions.userIdFor(token)

        // Assert
        assertEquals(maria, almost)
        assertNull(expired)
    }

    @Test
    fun revokeAllKeepsOnlyTheGivenSession() {
        // Arrange
        val phone = sessions.issue(maria)
        val laptop = sessions.issue(maria)
        val tablet = sessions.issue(maria)

        // Act
        sessions.revokeAll(maria, keepToken = phone)
        val afterKeeping = listOf(phone, laptop, tablet).map(sessions::userIdFor)
        sessions.revokeAll(maria)
        val afterAll = sessions.userIdFor(phone)

        // Assert
        assertEquals(listOf(maria, null, null), afterKeeping)
        assertNull(afterAll)
    }

    private class MovableClock(private var now: Instant) : Clock() {
        fun advance(by: Duration) {
            now = now.plus(by)
        }

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneId.of("America/Recife")

        override fun withZone(zone: ZoneId): Clock = this
    }

    private fun registration() = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = "",
        email = "maria@email.com",
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
