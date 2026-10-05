package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.AuthenticatedAccount

sealed interface AuthResult {
    data class Success(val account: AuthenticatedAccount) : AuthResult

    data class Failed(val error: AuthError) : AuthResult
}

sealed interface AuthError {
    data object NotImplemented : AuthError

    data class Rejected(val message: String) : AuthError
}
