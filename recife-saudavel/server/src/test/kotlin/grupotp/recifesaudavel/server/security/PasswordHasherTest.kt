package grupotp.recifesaudavel.server.security

import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHasherTest {
    private val hasher = PasswordHasher()

    @Test
    fun aHashVerifiesOnlyItsOwnPassword() {
        // Act
        val stored = hasher.hash("segredo")

        // Assert
        assertTrue(stored.startsWith("pbkdf2-sha256\$600000\$"))
        assertTrue(hasher.verify("segredo", stored))
        assertFalse(hasher.verify("Segredo", stored))
        assertFalse(hasher.needsRehash(stored))
    }

    @Test
    fun hashesWithFewerIterationsStillVerifyButAskForARehash() {
        // Arrange: a hash made by an older version with only 1000 iterations.
        val salt = "salt-salt-salt-1".toByteArray()
        val spec = PBEKeySpec("segredo".toCharArray(), salt, 1000, 256)
        val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val encoder = Base64.getEncoder()
        val old = "pbkdf2-sha256\$1000\$${encoder.encodeToString(salt)}\$${encoder.encodeToString(derived)}"

        // Act
        val verified = hasher.verify("segredo", old)
        val rehash = hasher.needsRehash(old)

        // Assert
        assertTrue(verified)
        assertTrue(rehash)
    }

    @Test
    fun malformedValuesNeverVerify() {
        // Arrange
        val values = listOf(
            "",
            "salt:hash",
            "pbkdf2-sha256\$0\$AAAA\$AAAA",
            "pbkdf2-sha256\$x\$AAAA\$AAAA",
            "pbkdf2-sha256\$1000\$not base64\$AAAA",
            "md5\$1000\$AAAA\$AAAA",
        )

        // Act
        val verified = values.map { hasher.verify("segredo", it) }

        // Assert
        assertTrue(verified.none { it })
    }
}
