package grupotp.medcall.app.usecase.auth

import grupotp.medcall.app.domain.auth.AuthResult
import grupotp.medcall.app.gateway.auth.AuthGateway

class RequestPasswordRecoveryUseCase(
    private val gateway: AuthGateway,
) {
    suspend operator fun invoke(email: String): AuthResult = gateway.requestPasswordRecovery(email)
}
