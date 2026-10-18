package grupotp.recifesaudavel.app.domain.auth

sealed interface AuthError {
    data object NotImplemented : AuthError

    data object Offline : AuthError

    data object SessionExpired : AuthError

    data class Rejected(val message: String) : AuthError
}
