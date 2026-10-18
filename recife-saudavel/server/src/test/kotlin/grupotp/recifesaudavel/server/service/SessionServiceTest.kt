package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.security.SecretString
import grupotp.recifesaudavel.server.database.DatabaseConnector
import grupotp.recifesaudavel.server.database.DatabaseSettings
import grupotp.recifesaudavel.server.database.table.SessionsTable
import grupotp.recifesaudavel.server.database.table.UsersTable
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
import kotlin.time.Duration.Companion.days
import kotlin.time.toKotlinInstant

private val IDLE: Duration = Duration.ofDays(180)

class SessionServiceTest {
    private val clock = MovableClock(Instant.parse("2026-10-12T13:00:00Z"))

    private lateinit var database: Database
    private lateinit var sessions: SessionService
    private var maria = 0

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
        maria = UserRepository(database).insert(registration(), "hash", clock.instant().toKotlinInstant())!!.id
    }

    @AfterTest
    fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun aSessionInUseNeverExpires() {
        // Arrange
        val session = sessions.open(maria)

        // Act: used every 100 days for two years, always within the 180-day idle timeout.
        val answers = (1..7).map {
            clock.advance(Duration.ofDays(100))
            sessions.userIdFor(session)
        }

        // Assert
        assertTrue(answers.all { it == maria })
    }

    @Test
    fun aSessionEndsAfterTheIdleTimeoutWithoutUse() {
        // Arrange
        val session = sessions.open(maria)

        // Act
        clock.advance(IDLE.minusMinutes(1))
        val almost = sessions.userIdFor(session)
        clock.advance(IDLE.minusMinutes(1))
        val stillUsed = sessions.userIdFor(session)
        clock.advance(IDLE)
        val expired = sessions.userIdFor(session)

        // Assert
        assertEquals(maria, almost)
        assertEquals(maria, stillUsed)
        assertNull(expired)
    }

    @Test
    fun withoutAnIdleTimeoutSessionsNeverExpire() {
        // Arrange
        val forever = SessionService(SessionRepository(database), clock, SessionSettings(idleTimeout = null))
        val session = forever.open(maria)

        // Act
        clock.advance(Duration.ofDays(3650))
        val answer = forever.userIdFor(session)

        // Assert
        assertEquals(maria, answer)
    }

    @Test
    fun revokeAllKeepsOnlyTheGivenSession() {
        // Arrange
        val phone = sessions.open(maria)
        val laptop = sessions.open(maria)
        val tablet = sessions.open(maria)

        // Act
        sessions.revokeAll(maria, keepId = phone.id)
        val afterKeeping = listOf(phone, laptop, tablet).map { sessions.userIdFor(it) }
        sessions.revokeAll(maria)
        val afterAll = sessions.userIdFor(phone)

        // Assert
        assertEquals(listOf(maria, null, null), afterKeeping)
        assertNull(afterAll)
    }

    @Test
    fun aTokenOfAnotherSessionWithTheSameIdIsRefused() {
        // Arrange: after a database reset, ids start over, but creation times do not repeat.
        val session = sessions.open(maria)

        // Act
        val sameSession = sessions.userIdFor(session.id, session.createdAt)
        val olderToken = sessions.userIdFor(session.id, session.createdAt - 1.days)

        // Assert
        assertEquals(maria, sameSession)
        assertNull(olderToken)
    }

    private fun SessionService.userIdFor(session: OpenedSession): Int? = userIdFor(session.id, session.createdAt)

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
