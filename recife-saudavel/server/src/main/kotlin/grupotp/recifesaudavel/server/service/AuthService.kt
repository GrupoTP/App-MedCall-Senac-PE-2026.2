package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.api.ErrorCode
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.security.PasswordHasher
import grupotp.recifesaudavel.server.security.SessionTokens
import grupotp.recifesaudavel.server.service.result.RegistrationResult
import grupotp.recifesaudavel.server.domain.ServiceError
import grupotp.recifesaudavel.server.service.result.SignInResult
import java.time.Clock
import kotlin.time.toKotlinInstant

class AuthService(
    private val users: UserRepository,
    private val passwords: PasswordHasher,
    private val sessions: SessionService,
    private val tokens: SessionTokens,
    private val clock: Clock,
) {
    fun register(request: RegisterAccountRequest): RegistrationResult {
        validate(request)?.let { return RegistrationResult.Rejected(it) }
        val email = request.email.trim().lowercase()
        // Answers the common case without paying for the password hash; insert covers the race.
        if (users.findByEmail(email) != null) {
            return RegistrationResult.Rejected(ServiceError(ErrorCode.EMAIL_TAKEN))
        }
        val account = users.insert(
            request = request,
            passwordHash = passwords.hash(request.password),
            now = clock.instant().toKotlinInstant(),
        ) ?: return RegistrationResult.Rejected(ServiceError(ErrorCode.EMAIL_TAKEN))
        return RegistrationResult.Created(account.withSession())
    }

    fun signIn(request: SignInRequest): SignInResult {
        val email = request.email.trim().lowercase()
        val user = users.findByEmail(email)
        if (user == null || !passwords.verify(request.password, user.passwordHash)) {
            return SignInResult.Rejected
        }
        if (passwords.needsRehash(user.passwordHash)) {
            users.updatePasswordHash(user.account.id, passwords.hash(request.password))
        }
        users.recordLogin(user.account.id, clock.instant().toKotlinInstant())
        return SignInResult.Authenticated(user.account.withSession())
    }

    /**
     * Current profile of a signed-in user, without a token: the client keeps the one it has.
     * Lets a device saved long ago catch up with what the server holds.
     */
    fun profileOf(userId: Int): AuthenticatedAccount? = users.findById(userId)

    private fun AuthenticatedAccount.withSession(): AuthenticatedAccount =
        copy(sessionToken = tokens.sign(userId = id, session = sessions.open(id)))

    private fun validate(request: RegisterAccountRequest): ServiceError? = when {
        request.fullName.isBlank() -> ServiceError(ErrorCode.FULL_NAME_REQUIRED)
        !request.email.contains("@") -> ServiceError(ErrorCode.EMAIL_INVALID)
        request.password.value.length < MIN_PASSWORD -> ServiceError(
            ErrorCode.PASSWORD_TOO_SHORT,
            MIN_PASSWORD
        )
        request.primaryPhone.isEmpty() -> ServiceError(ErrorCode.PRIMARY_PHONE_REQUIRED)
        !request.primaryPhone.hasValidLength || !request.secondaryPhone.hasValidLength ||
            !request.emergencyContactPhone.hasValidLength -> ServiceError(ErrorCode.PHONE_INVALID)
        !request.acceptedPrivacyPolicy -> ServiceError(ErrorCode.PRIVACY_POLICY_REQUIRED)
        !request.acceptedTreatment -> ServiceError(ErrorCode.TREATMENT_CONSENT_REQUIRED)
        else -> null
    }

    private companion object {
        const val MIN_PASSWORD = 6
    }
}
