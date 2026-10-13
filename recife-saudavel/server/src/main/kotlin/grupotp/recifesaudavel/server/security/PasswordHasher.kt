package grupotp.recifesaudavel.server.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores passwords as salt and PBKDF2 hash. The plain password is never persisted.
 */
class PasswordHasher {
    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val derived = derive(password, salt)
        return encode(salt) + SEPARATOR + encode(derived)
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(SEPARATOR, limit = 2)
        if (parts.size != 2) {
            return false
        }
        val salt = decode(parts[0])
        val expected = decode(parts[1])
        return MessageDigest.isEqual(derive(password, salt), expected)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ITERATIONS = 12_000
        const val KEY_BITS = 128
        const val SALT_BYTES = 16
        const val SEPARATOR = ":"
        val random = SecureRandom()
    }
}
