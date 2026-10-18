package grupotp.recifesaudavel.app.usecase.auth

import grupotp.recifesaudavel.app.domain.auth.AuthGateway
import grupotp.recifesaudavel.app.domain.auth.AuthResult

class RequestPasswordRecoveryUseCase(
    private val gateway: AuthGateway,
) {
    suspend operator fun invoke(email: String): AuthResult = gateway.requestPasswordRecovery(email)
}
