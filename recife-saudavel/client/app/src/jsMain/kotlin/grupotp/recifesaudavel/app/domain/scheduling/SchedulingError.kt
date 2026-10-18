package grupotp.recifesaudavel.app.domain.scheduling

sealed interface SchedulingError {
    /** The server could not be reached. Scheduling needs the network. */
    data object Offline : SchedulingError

    /** The saved token is missing or no longer valid. The person must sign in again. */
    data object SessionExpired : SchedulingError

    data class Rejected(val message: String) : SchedulingError
}
