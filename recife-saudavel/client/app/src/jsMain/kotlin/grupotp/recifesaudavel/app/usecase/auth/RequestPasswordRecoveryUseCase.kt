package grupotp.recifesaudavel.app.usecase.auth

import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.app.gateway.auth.AuthGateway

class RequestPasswordRecoveryUseCase(
    private val gateway: AuthGateway,
) {
    suspend operator fun invoke(email: String): AuthResult = gateway.requestPasswordRecovery(email)
}
