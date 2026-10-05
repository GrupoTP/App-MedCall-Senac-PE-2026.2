package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.common.SignInRequest

/**
 * Boundary the authentication use cases call.
 */
interface AuthGateway {
    suspend fun signIn(request: SignInRequest): AuthResult

    suspend fun register(request: RegisterAccountRequest): AuthResult

    suspend fun requestPasswordRecovery(email: String): AuthResult
}
