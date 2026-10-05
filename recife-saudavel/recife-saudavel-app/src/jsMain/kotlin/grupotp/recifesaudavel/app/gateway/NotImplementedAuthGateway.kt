package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.usecase.AuthError
import grupotp.recifesaudavel.app.usecase.AuthGateway
import grupotp.recifesaudavel.app.usecase.AuthResult
import grupotp.recifesaudavel.app.usecase.RegisterAccountCommand
import grupotp.recifesaudavel.app.usecase.SignInCommand

/**
 * Authentication adapter used until the server endpoints exist.
 */
class NotImplementedAuthGateway : AuthGateway {
    override fun signIn(command: SignInCommand): AuthResult = notImplemented()

    override fun register(command: RegisterAccountCommand): AuthResult = notImplemented()

    override fun requestPasswordRecovery(email: String): AuthResult = notImplemented()

    private fun notImplemented(): AuthResult = AuthResult.Failed(AuthError.NotImplemented)
}
