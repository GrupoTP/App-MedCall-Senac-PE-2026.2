package grupotp.recifesaudavel.app.usecase.auth

import grupotp.recifesaudavel.app.domain.auth.AuthGateway
import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest

class RegisterAccountUseCase(
    private val gateway: AuthGateway,
) {
    suspend operator fun invoke(request: RegisterAccountRequest): AuthResult = gateway.register(request)
}
