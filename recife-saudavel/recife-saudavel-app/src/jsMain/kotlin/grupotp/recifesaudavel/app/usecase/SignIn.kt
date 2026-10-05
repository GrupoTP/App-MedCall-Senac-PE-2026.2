package grupotp.recifesaudavel.app.usecase

/**
 * Credentials collected by the sign-in screen.
 */
data class SignInCommand(
    val email: String,
    val password: String,
)

/**
 * Asks the gateway to sign the person in.
 */
class SignIn(
    private val authGateway: AuthGateway,
) {
    operator fun invoke(command: SignInCommand): AuthResult = authGateway.signIn(command)
}
