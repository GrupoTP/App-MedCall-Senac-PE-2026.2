package grupotp.recifesaudavel.app.usecase

/**
 * Asks the gateway to send password recovery instructions.
 */
class RequestPasswordRecovery(
    private val authGateway: AuthGateway,
) {
    operator fun invoke(email: String): AuthResult = authGateway.requestPasswordRecovery(email)
}
