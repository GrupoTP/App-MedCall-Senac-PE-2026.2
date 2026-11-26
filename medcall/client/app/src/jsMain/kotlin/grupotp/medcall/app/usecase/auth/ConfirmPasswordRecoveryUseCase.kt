package grupotp.medcall.app.usecase.auth

import grupotp.medcall.app.domain.auth.RecoveryResult
import grupotp.medcall.app.gateway.auth.AuthGateway
import grupotp.medcall.common.auth.ConfirmPasswordRecoveryRequest

class ConfirmPasswordRecoveryUseCase(
    private val gateway: AuthGateway,
) {
    suspend operator fun invoke(request: ConfirmPasswordRecoveryRequest): RecoveryResult =
        gateway.confirmPasswordRecovery(request)
}
