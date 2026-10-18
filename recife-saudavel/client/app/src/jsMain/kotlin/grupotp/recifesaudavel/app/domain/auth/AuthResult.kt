package grupotp.recifesaudavel.app.domain.auth

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

sealed interface AuthResult {
    data class Success(val account: AuthenticatedAccount) : AuthResult

    data class Failed(val error: AuthError) : AuthResult
}
