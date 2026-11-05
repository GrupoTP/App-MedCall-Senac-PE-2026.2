package grupotp.medcall.app.usecase.auth

import grupotp.medcall.app.domain.auth.AuthResult
import grupotp.medcall.app.gateway.auth.AuthGateway
import grupotp.medcall.common.auth.SignInRequest

class SignInUseCase(
    private val gateway: AuthGateway,
) {
    suspend operator fun invoke(request: SignInRequest): AuthResult = gateway.signIn(request)
}
