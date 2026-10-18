package grupotp.recifesaudavel.server.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores passwords as PBKDF2 hashes. The plain password is never persisted.
 *
 * The stored value is `pbkdf2-sha256$<iterations>$<salt>$<hash>`: each hash keeps the
 * iteration count it was made with, so raising [ITERATIONS] later keeps old hashes valid,
 * and [needsRehash] tells when one should be replaced at the next sign-in.
 */
class PasswordHasher {
    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val derived = derive(password, salt, ITERATIONS)
        return listOf(SCHEME, ITERATIONS.toString(), encode(salt), encode(derived)).joinToString(SEPARATOR)
    }

    fun verify(password: String, stored: String): Boolean {
        val parsed = parse(stored) ?: return false
        return MessageDigest.isEqual(derive(password, parsed.salt, parsed.iterations), parsed.hash)
    }

    /** True when [stored] was made with fewer iterations than the current ones. */
    fun needsRehash(stored: String): Boolean = (parse(stored)?.iterations ?: 0) < ITERATIONS

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

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    private companion object {
        const val SCHEME = "pbkdf2-sha256"
        const val ALGORITHM = "PBKDF2WithHmacSHA256"

        /** OWASP Password Storage Cheat Sheet recommendation for PBKDF2-HMAC-SHA256. */
        const val ITERATIONS = 600_000
        const val KEY_BITS = 256
        const val SALT_BYTES = 16
        const val SEPARATOR = "$"
        val random = SecureRandom()
    }
}
