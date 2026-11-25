package grupotp.medcall.server.service

import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.auth.ConfirmPasswordRecoveryRequest
import grupotp.medcall.server.domain.ServiceError
import grupotp.medcall.server.mail.RecoveryMail
import grupotp.medcall.server.repository.PasswordResetRepository
import grupotp.medcall.server.repository.UserRepository
import grupotp.medcall.server.security.PasswordHasher
import grupotp.medcall.server.service.result.RecoveryResult
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinInstant

/**
 * One-time password recovery.
 * The answer does not say whether the e-mail exists.
 * The plain code is mailed and only its SHA-256 is stored.
 * A successful reset ends every session of the account.
 *
 * https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html
 */
class PasswordRecoveryService(
    private val users: UserRepository,
    private val passwords: PasswordHasher,
    private val sessions: SessionService,
    private val resets: PasswordResetRepository,
    private val mail: RecoveryMail,
    private val clock: Clock,
) {
    fun request(email: String): RecoveryResult {
        val normalized = email.trim().lowercase()
        if (!looksLikeEmail(normalized)) {
            return RecoveryResult.Rejected(ServiceError(ErrorCode.EMAIL_INVALID))
        }
        if (!mail.configured) {
            return RecoveryResult.Unavailable
        }
        val user = users.findByEmail(normalized) ?: return RecoveryResult.Accepted
        val code = newCode()
        resets.replace(
            userId = user.account.id,
            codeHash = hash(code),
            expiresAt = clock.instant().toKotlinInstant() + CODE_LIFETIME,
        )
        return try {
            mail.send(normalized, code)
            RecoveryResult.Accepted
        } catch (_: Exception) {
            RecoveryResult.Unavailable
        }
    }

    fun confirm(request: ConfirmPasswordRecoveryRequest): RecoveryResult {
        val email = request.email.trim().lowercase()
        if (!looksLikeEmail(email)) {
            return RecoveryResult.Rejected(ServiceError(ErrorCode.EMAIL_INVALID))
        }
        if (request.newPassword.value.length < MIN_PASSWORD) {
            return RecoveryResult.Rejected(ServiceError(ErrorCode.PASSWORD_TOO_SHORT, MIN_PASSWORD))
        }
        val user = users.findByEmail(email) ?: return RecoveryResult.Rejected(ServiceError(ErrorCode.RECOVERY_CODE_INVALID))
        val consumed = resets.consume(
            userId = user.account.id,
            codeHash = hash(request.code.value.trim()),
            now = clock.instant().toKotlinInstant(),
        )
        if (!consumed) {
            return RecoveryResult.Rejected(ServiceError(ErrorCode.RECOVERY_CODE_INVALID))
        }
        users.updatePasswordHash(user.account.id, passwords.hash(request.newPassword))
        sessions.revokeAll(user.account.id)
        return RecoveryResult.Accepted
    }

    private fun looksLikeEmail(email: String): Boolean =
        email.contains("@") && !email.startsWith("@") && !email.endsWith("@")

    private fun newCode(): String {
        val bytes = ByteArray(CODE_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(code.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MIN_PASSWORD = 6
        const val CODE_BYTES = 32
        val CODE_LIFETIME = 30.minutes
        val random = SecureRandom()
    }
}
