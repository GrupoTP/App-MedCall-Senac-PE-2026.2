package grupotp.recifesaudavel.app.usecase

/**
 * Boundary the authentication use cases call.
 * The interface stays free of HTTP so a later adapter can reach the server.
 */
interface AuthGateway {
    fun signIn(command: SignInCommand): AuthResult

    fun register(command: RegisterAccountCommand): AuthResult

    fun requestPasswordRecovery(email: String): AuthResult
}
