package grupotp.recifesaudavel.app.domain.scheduling

/**
 * Outcome of a call to the server that needs the session token.
 */
sealed interface SchedulingResult<out T> {
    data class Success<T>(val value: T) : SchedulingResult<T>

    data class Failed(val error: SchedulingError) : SchedulingResult<Nothing>
}
