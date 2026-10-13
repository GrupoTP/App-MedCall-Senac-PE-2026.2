package grupotp.recifesaudavel.app.usecase

class RequestPasswordRecovery(
    private val authGateway: AuthGateway,
) {
    suspend operator fun invoke(email: String): AuthResult = authGateway.requestPasswordRecovery(email)
}
