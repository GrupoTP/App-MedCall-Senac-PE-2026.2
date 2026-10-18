package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.server.repository.SessionRepository
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.time.toKotlinDuration
import kotlin.time.toKotlinInstant

/**
 * Opens and resolves sessions. The client holds a signed token naming the session
 * ([SessionTokens][grupotp.recifesaudavel.server.security.SessionTokens]); this service decides
 * whether that session is still open.
 *
 * A session lasts while it is used: it only expires after [SessionSettings.idleTimeout] without
 * any request, and never when that is null.
 */
class SessionService(
    private val sessions: SessionRepository,
    private val clock: Clock,
    private val settings: SessionSettings,
) {
    fun open(userId: Int): OpenedSession {
        val now = now()
        // Housekeeping: expired rows are useless, and sign-ins are rare enough to pay for it.
        idleTimeout()?.let { sessions.deleteUnusedSince(now - it) }
        // Whole seconds, like the token's `iat`, so the two compare equal.
        val createdAt = Instant.fromEpochSeconds(now.epochSeconds)
        return OpenedSession(id = sessions.insert(userId, createdAt), createdAt = createdAt)
    }

    /**
     * User id of session [id], or null when it is unknown, revoked or expired.
     *
     * [createdAt] must match too: ids are sequential, so after a database reset a new session can
     * get the id an old token names, but not its creation time.
     */
    fun userIdFor(id: Int, createdAt: Instant): Int? {
        val session = sessions.find(id) ?: return null
        if (session.createdAt != createdAt) {
            return null
        }
        val now = now()
        val idle = now - session.lastUsedAt
        val timeout = idleTimeout()
        if (timeout != null && idle >= timeout) {
            sessions.delete(id)
            return null
        }
        // Writing on every request would cost a database write per call; once a day is enough
        // for a timeout counted in days.
        if (idle >= TOUCH_INTERVAL) {
            sessions.touch(id, now)
        }
        return session.userId
    }

    /** Ends session [id] from now on. Unknown ids are ignored. */
    fun revoke(id: Int) {
        sessions.delete(id)
    }

    /** Ends every session of [userId], on every device, except [keepId] when given. */
    fun revokeAll(userId: Int, keepId: Int? = null) {
        sessions.deleteAllOf(userId, keepId)
    }

    private fun now(): Instant = clock.instant().toKotlinInstant()

    private fun idleTimeout(): Duration? = settings.idleTimeout?.toKotlinDuration()

    companion object {
        /** How often the last use is written down. */
        val TOUCH_INTERVAL: Duration = 1.days
    }
}
