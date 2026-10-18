package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.security.PasswordHasher
import grupotp.recifesaudavel.server.security.SessionTokens
import java.time.Clock
import java.util.UUID

sealed interface RegistrationResult {
    data class Created(val account: AuthenticatedAccount) : RegistrationResult
    data class Rejected(val message: String) : RegistrationResult
}

sealed interface SignInResult {
    data class Authenticated(val account: AuthenticatedAccount) : SignInResult
    data class Rejected(val message: String) : SignInResult
}

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
            return RegistrationResult.Rejected(EMAIL_TAKEN)
        }
        val account = users.insert(
            id = UUID.randomUUID().toString(),
            request = request,
            passwordHash = passwords.hash(request.password),
            now = clock.millis(),
        ) ?: return RegistrationResult.Rejected(EMAIL_TAKEN)
        return RegistrationResult.Created(account.withSession())
    }

    fun signIn(request: SignInRequest): SignInResult {
        val email = request.email.trim().lowercase()
        val user = users.findByEmail(email)
        if (user == null || !passwords.verify(request.password, user.passwordHash)) {
            return SignInResult.Rejected("E-mail ou senha incorretos.")
        }
        if (passwords.needsRehash(user.passwordHash)) {
            users.updatePasswordHash(user.account.id, passwords.hash(request.password))
        }
        users.recordLogin(user.account.id, clock.millis())
        return SignInResult.Authenticated(user.account.withSession())
    }

    /**
     * Current profile of a signed-in user, without a token: the client keeps the one it has.
     * Lets a device saved long ago catch up with what the server holds.
     */
    fun profileOf(userId: String): AuthenticatedAccount? = users.findById(userId)

    private fun AuthenticatedAccount.withSession(): AuthenticatedAccount =
        copy(sessionToken = tokens.sign(userId = id, sessionId = sessions.open(id)))

    private fun validate(request: RegisterAccountRequest): String? = when {
        request.fullName.isBlank() -> "Nome completo é obrigatório."
        !request.email.contains("@") -> "Informe um e-mail válido."
        request.password.value.length < 6 -> "Mínimo 6 caracteres."
        request.primaryPhone.isEmpty() -> "Telefone principal é obrigatório."
        !request.primaryPhone.hasValidLength || !request.secondaryPhone.hasValidLength ||
            !request.emergencyContactPhone.hasValidLength -> "Telefone inválido."
        !request.acceptedPrivacyPolicy -> "Leitura da Política de Privacidade é obrigatória."
        !request.acceptedTreatment -> "Consentimento de tratamento de dados é necessário."
        else -> null
    }

    private companion object {
        const val EMAIL_TAKEN = "E-mail já cadastrado."
    }
}
