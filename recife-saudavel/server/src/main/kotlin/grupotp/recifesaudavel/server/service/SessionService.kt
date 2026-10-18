package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.server.repository.SessionRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.Base64

/**
 * Issues and resolves bearer tokens. The client keeps the token with the signed-in profile;
 * the database only keeps its hash.
 *
 * A token lasts while it is used: it only expires after [SessionSettings.idleTimeout] without
 * any request, and never when that is null.
 */
class SessionService(
    private val sessions: SessionRepository,
    private val clock: Clock,
    private val settings: SessionSettings,
) {
    fun issue(userId: String): String {
        val bytes = ByteArray(TOKEN_BYTES).also { random.nextBytes(it) }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val now = clock.millis()
        // Housekeeping: expired rows are useless, and sign-ins are rare enough to pay for it.
        settings.idleTimeout?.let { sessions.deleteUnusedSince(now - it.toMillis()) }
        sessions.insert(hash(token), userId, now)
        return token
    }

    /** User id of [token], or null when it is unknown, revoked or expired. */
    fun userIdFor(token: String): String? {
        if (token.isBlank()) {
            return null
        }
        val tokenHash = hash(token)
        val session = sessions.find(tokenHash) ?: return null
        val now = clock.millis()
        val idle = now - session.lastUsedAt
        val timeout = settings.idleTimeout
        if (timeout != null && idle >= timeout.toMillis()) {
            sessions.delete(tokenHash)
            return null
        }
        // Writing on every request would cost a database write per call; once a day is enough
        // for a timeout counted in days.
        if (idle >= TOUCH_INTERVAL.toMillis()) {
            sessions.touch(tokenHash, now)
        }
        return session.userId
    }

    /** Makes [token] useless from now on. Unknown tokens are ignored. */
    fun revoke(token: String) {
        if (token.isNotBlank()) {
            sessions.delete(hash(token))
        }
    }

    /** Ends every session of [userId], on every device, except [keepToken] when given. */
    fun revokeAll(userId: String, keepToken: String? = null) {
        sessions.deleteAllOf(userId, keepToken?.takeIf { it.isNotBlank() }?.let(::hash))
    }

    private fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        /** How often the last use is written down. */
        val TOUCH_INTERVAL: Duration = Duration.ofDays(1)
        private const val TOKEN_BYTES = 32
        private val random = SecureRandom()
    }
}
