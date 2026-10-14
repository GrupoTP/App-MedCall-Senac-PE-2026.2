package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.server.repository.SessionRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64

/**
 * Issues and resolves bearer tokens. The client keeps the token with the signed-in profile;
 * the database only keeps its hash.
 */
class SessionService(
    private val sessions: SessionRepository,
    private val clock: Clock,
) {
    fun issue(userId: String): String {
        val bytes = ByteArray(TOKEN_BYTES).also { random.nextBytes(it) }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        sessions.insert(hash(token), userId, clock.millis())
        return token
    }

    fun userIdFor(token: String): String? {
        if (token.isBlank()) {
            return null
        }
        return sessions.findUserId(hash(token))
    }

    private fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val TOKEN_BYTES = 32
        val random = SecureRandom()
    }
}
