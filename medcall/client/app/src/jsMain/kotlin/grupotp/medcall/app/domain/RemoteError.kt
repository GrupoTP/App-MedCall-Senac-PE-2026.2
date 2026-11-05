package grupotp.recifesaudavel.app.domain

sealed interface RemoteError {
    /** The server could not be reached. */
    data object Offline : RemoteError

    /** The saved token is missing or no longer valid. The person must sign in again. */
    data object SessionExpired : RemoteError

    data class Rejected(val message: String) : RemoteError
}
