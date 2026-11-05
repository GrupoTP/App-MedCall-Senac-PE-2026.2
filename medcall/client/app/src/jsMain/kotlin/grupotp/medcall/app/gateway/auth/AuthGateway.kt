package grupotp.medcall.app.gateway.auth

import grupotp.medcall.app.domain.auth.AuthError
import grupotp.medcall.app.domain.auth.AuthResult
import grupotp.medcall.common.auth.RegisterAccountRequest
import grupotp.medcall.common.auth.SignInRequest
import grupotp.medcall.common.security.SecretString

/**
 * Authentication API of the server.
 */
interface AuthGateway {
    suspend fun signIn(request: SignInRequest): AuthResult

    suspend fun register(request: RegisterAccountRequest): AuthResult

    suspend fun requestPasswordRecovery(email: String): AuthResult

    /**
     * Profile the server holds for [token]. A failed result means keep what the device has:
     * [AuthError.Offline] when the server cannot be reached, [AuthError.SessionExpired] when the token is no longer valid.
     */
    suspend fun profile(token: SecretString): AuthResult
}
