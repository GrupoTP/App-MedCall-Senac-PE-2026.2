package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.RegisterAccountRequest

class RegisterAccount(
    private val authGateway: AuthGateway,
) {
    suspend operator fun invoke(request: RegisterAccountRequest): AuthResult = authGateway.register(request)
}
