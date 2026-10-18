package grupotp.recifesaudavel.app.fake

import grupotp.recifesaudavel.app.domain.auth.AuthError
import grupotp.recifesaudavel.app.domain.auth.AuthGateway
import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Answers each call with the result it was given and records the requests.
 */
internal class FakeAuthGateway(
    private val signInResult: AuthResult = AuthResult.Failed(AuthError.NotImplemented),
    private val registerResult: AuthResult = AuthResult.Failed(AuthError.NotImplemented),
    private val recoveryResult: AuthResult = AuthResult.Failed(AuthError.NotImplemented),
    private val profileResult: AuthResult = AuthResult.Failed(AuthError.Offline),
) : AuthGateway {
    var signInRequest: SignInRequest? = null
    var registerRequest: RegisterAccountRequest? = null

    override suspend fun signIn(request: SignInRequest): AuthResult {
        signInRequest = request
        return signInResult
    }

    override suspend fun register(request: RegisterAccountRequest): AuthResult {
        registerRequest = request
        return registerResult
    }

    override suspend fun requestPasswordRecovery(email: String): AuthResult = recoveryResult

    override suspend fun profile(token: SecretString): AuthResult = profileResult
}
