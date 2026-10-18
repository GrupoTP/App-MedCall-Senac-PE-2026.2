package grupotp.recifesaudavel.app.domain.auth

import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString

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
