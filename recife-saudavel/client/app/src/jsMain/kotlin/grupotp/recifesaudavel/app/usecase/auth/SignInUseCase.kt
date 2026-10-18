package grupotp.recifesaudavel.app.usecase.auth

import grupotp.recifesaudavel.app.domain.auth.AuthGateway
import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.common.auth.SignInRequest

class SignInUseCase(
    private val gateway: AuthGateway,
) {
    suspend operator fun invoke(request: SignInRequest): AuthResult = gateway.signIn(request)
}
