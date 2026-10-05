package grupotp.recifesaudavel.app.usecase

/**
 * Outcome of an authentication use case.
 */
sealed interface AuthResult {
    data object Success : AuthResult

    data class Failed(
        val error: AuthError,
    ) : AuthResult
}

/**
 * Reason an authentication use case did not complete.
 */
sealed interface AuthError {
    data object NotImplemented : AuthError
}
