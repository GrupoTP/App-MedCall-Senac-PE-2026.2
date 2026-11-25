package grupotp.medcall.app.fake

import grupotp.medcall.app.domain.auth.AuthError
import grupotp.medcall.app.domain.auth.AuthResult
import grupotp.medcall.app.domain.auth.RecoveryResult
import grupotp.medcall.app.gateway.auth.AuthGateway
import grupotp.medcall.common.auth.ConfirmPasswordRecoveryRequest
import grupotp.medcall.common.auth.RegisterAccountRequest
import grupotp.medcall.common.auth.SignInRequest
import grupotp.medcall.common.security.SecretString

/**
 * Answers each call with the result it was given and records the requests.
 */
internal class FakeAuthGateway(
    private val signInResult: AuthResult = AuthResult.Failed(AuthError.NotImplemented),
    private val registerResult: AuthResult = AuthResult.Failed(AuthError.NotImplemented),
    private val recoveryResult: RecoveryResult = RecoveryResult.Accepted,
    private val confirmResult: RecoveryResult = RecoveryResult.Accepted,
    private val profileResult: AuthResult = AuthResult.Failed(AuthError.Offline),
) : AuthGateway {
    var signInRequest: SignInRequest? = null
    var registerRequest: RegisterAccountRequest? = null
    var recoveryEmail: String? = null
    var confirmRequest: ConfirmPasswordRecoveryRequest? = null

    override suspend fun signIn(request: SignInRequest): AuthResult {
        signInRequest = request
        return signInResult
    }

    override suspend fun register(request: RegisterAccountRequest): AuthResult {
        registerRequest = request
        return registerResult
    }

    override suspend fun requestPasswordRecovery(email: String): RecoveryResult {
        recoveryEmail = email
        return recoveryResult
    }

    override suspend fun confirmPasswordRecovery(request: ConfirmPasswordRecoveryRequest): RecoveryResult {
        confirmRequest = request
        return confirmResult
    }

    override suspend fun profile(token: SecretString): AuthResult = profileResult
}
