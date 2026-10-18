package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.auth.SignInRequest

class SignIn(
    private val authGateway: AuthGateway,
) {
    suspend operator fun invoke(request: SignInRequest): AuthResult = authGateway.signIn(request)
}
