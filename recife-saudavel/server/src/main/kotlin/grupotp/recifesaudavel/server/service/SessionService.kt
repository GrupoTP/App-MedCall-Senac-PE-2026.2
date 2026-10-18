package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.server.repository.SessionRepository
import java.time.Clock
import java.time.Duration
import java.util.UUID

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
    /** Starts a session of [userId] and returns its id, to be signed into the token. */
    fun open(userId: String): String {
        val id = UUID.randomUUID().toString()
        val now = clock.millis()
        // Housekeeping: expired rows are useless, and sign-ins are rare enough to pay for it.
        settings.idleTimeout?.let { sessions.deleteUnusedSince(now - it.toMillis()) }
        sessions.insert(id, userId, now)
        return id
    }

    /** User id of session [id], or null when it is unknown, revoked or expired. */
    fun userIdFor(id: String): String? {
        if (id.isBlank()) {
            return null
        }
        val session = sessions.find(id) ?: return null
        val now = clock.millis()
        val idle = now - session.lastUsedAt
        val timeout = settings.idleTimeout
        if (timeout != null && idle >= timeout.toMillis()) {
            sessions.delete(id)
            return null
        }
        // Writing on every request would cost a database write per call; once a day is enough
        // for a timeout counted in days.
        if (idle >= TOUCH_INTERVAL.toMillis()) {
            sessions.touch(id, now)
        }
        return session.userId
    }

    /** Ends session [id] from now on. Unknown ids are ignored. */
    fun revoke(id: String) {
        sessions.delete(id)
    }

    /** Ends every session of [userId], on every device, except [keepId] when given. */
    fun revokeAll(userId: String, keepId: String? = null) {
        sessions.deleteAllOf(userId, keepId)
    }

    companion object {
        /** How often the last use is written down. */
        val TOUCH_INTERVAL: Duration = Duration.ofDays(1)
    }
}
