package grupotp.recifesaudavel.app.domain

/**
 * Outcome of a call to the server that needs the session token.
 */
sealed interface RemoteResult<out T> {
    data class Success<T>(val value: T) : RemoteResult<T>

    data class Failed(val error: RemoteError) : RemoteResult<Nothing>
}
