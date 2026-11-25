package grupotp.medcall.server.security

import grupotp.medcall.common.security.SecretString
import io.ktor.server.config.ApplicationConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores passwords as PBKDF2 hashes. The plain password is never persisted.
 *
 * The stored value is `pbkdf2-sha256$<iterations>$<salt>$<hash>`: each hash keeps the
 * iteration count it was made with, so raising [iterations] later keeps old hashes valid,
 * and [needsRehash] tells when one should be replaced at the next sign-in.
 *
 * @param iterations PBKDF2-HMAC-SHA256 rounds. Production uses [PRODUCTION_ITERATIONS].
 */
class PasswordHasher(
    private val iterations: Int = PRODUCTION_ITERATIONS,
) {
    init {
        require(iterations >= MIN_ITERATIONS) {
            "password iterations must be at least $MIN_ITERATIONS, got $iterations."
        }
    }

    fun hash(password: SecretString): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val derived = derive(password, salt, iterations)
        return listOf(SCHEME, iterations.toString(), encode(salt), encode(derived)).joinToString(SEPARATOR)
    }

    fun verify(password: SecretString, stored: String): Boolean {
        val parsed = parse(stored) ?: return false
        return MessageDigest.isEqual(derive(password, parsed.salt, parsed.iterations), parsed.hash)
    }

    /** True when [stored] was made with fewer iterations than the current ones. */
    fun needsRehash(stored: String): Boolean = (parse(stored)?.iterations ?: 0) < iterations

    private class Parsed(val iterations: Int, val salt: ByteArray, val hash: ByteArray)

    private fun parse(stored: String): Parsed? {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 4 || parts[0] != SCHEME) {
            return null
        }
        val iterations = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        return try {
            Parsed(iterations, decode(parts[2]), decode(parts[3]))
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun derive(password: SecretString, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.value.toCharArray(), salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    companion object {
        /** OWASP Password Storage Cheat Sheet recommendation for PBKDF2-HMAC-SHA256. */
        const val PRODUCTION_ITERATIONS = 600_000

        /** Reads `password.iterations` (`PASSWORD_ITERATIONS`). */
        fun from(config: ApplicationConfig): PasswordHasher {
            val raw = config.property("password.iterations").getString().trim()
            val iterations = raw.toIntOrNull()
                ?: throw IllegalArgumentException("password.iterations must be an integer, got $raw.")
            return PasswordHasher(iterations)
        }

        private const val SCHEME = "pbkdf2-sha256"
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val MIN_ITERATIONS = 1_000
        private const val KEY_BITS = 256
        private const val SALT_BYTES = 16
        private const val SEPARATOR = "$"
        private val random = SecureRandom()
    }
}
