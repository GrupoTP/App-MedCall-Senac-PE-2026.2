package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.common.SignInRequest
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.security.PasswordHasher
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
) {
    fun register(request: RegisterAccountRequest): RegistrationResult {
        validate(request)?.let { return RegistrationResult.Rejected(it) }
        val email = request.email.trim().lowercase()
        if (users.findByEmail(email) != null) {
            return RegistrationResult.Rejected("E-mail já cadastrado.")
        }
        val account = users.insert(
            id = UUID.randomUUID().toString(),
            request = request,
            passwordHash = passwords.hash(request.password),
        )
        return RegistrationResult.Created(account.withSession())
    }

    fun signIn(request: SignInRequest): SignInResult {
        val email = request.email.trim().lowercase()
        val user = users.findByEmail(email)
        if (user == null || !passwords.verify(request.password, user.passwordHash)) {
            return SignInResult.Rejected("E-mail ou senha incorretos.")
        }
        return SignInResult.Authenticated(user.account.withSession())
    }

    private fun AuthenticatedAccount.withSession(): AuthenticatedAccount = copy(sessionToken = sessions.issue(id))

    private fun validate(request: RegisterAccountRequest): String? = when {
        request.fullName.isBlank() -> "Nome completo é obrigatório."
        !request.email.contains("@") -> "Informe um e-mail válido."
        request.password.length < 6 -> "Mínimo 6 caracteres."
        request.primaryPhone.isBlank() -> "Telefone principal é obrigatório."
        !request.acceptedPrivacyPolicy -> "Leitura da Política de Privacidade é obrigatória."
        !request.acceptedTreatment -> "Consentimento de tratamento de dados é necessário."
        else -> null
    }
}
