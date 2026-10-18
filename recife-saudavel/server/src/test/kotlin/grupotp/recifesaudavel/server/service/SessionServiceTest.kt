package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.security.SecretString
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
import kotlin.test.assertTrue

private val IDLE: Duration = Duration.ofDays(180)

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
        sessions = SessionService(SessionRepository(database), clock, SessionSettings(IDLE))
        maria = UserRepository(database).insert(UUID.randomUUID().toString(), registration(), "hash")!!.id
    }

    @AfterTest
    fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun aTokenInUseNeverExpires() {
        // Arrange
        val token = sessions.issue(maria).value

        // Act: used every 100 days for two years, always within the 180-day idle timeout.
        val answers = (1..7).map {
            clock.advance(Duration.ofDays(100))
            sessions.userIdFor(token)
        }

        // Assert
        assertTrue(answers.all { it == maria })
    }

    @Test
    fun aTokenStopsWorkingAfterTheIdleTimeoutWithoutUse() {
        // Arrange
        val token = sessions.issue(maria).value

        // Act
        clock.advance(IDLE.minusMinutes(1))
        val almost = sessions.userIdFor(token)
        clock.advance(IDLE.minusMinutes(1))
        val stillUsed = sessions.userIdFor(token)
        clock.advance(IDLE)
        val expired = sessions.userIdFor(token)

        // Assert
        assertEquals(maria, almost)
        assertEquals(maria, stillUsed)
        assertNull(expired)
    }

    @Test
    fun withoutAnIdleTimeoutTokensNeverExpire() {
        // Arrange
        val forever = SessionService(SessionRepository(database), clock, SessionSettings(idleTimeout = null))
        val token = forever.issue(maria).value

        // Act
        clock.advance(Duration.ofDays(3650))
        val answer = forever.userIdFor(token)

        // Assert
        assertEquals(maria, answer)
    }

    @Test
    fun revokeAllKeepsOnlyTheGivenSession() {
        // Arrange
        val phone = sessions.issue(maria).value
        val laptop = sessions.issue(maria).value
        val tablet = sessions.issue(maria).value

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
