package grupotp.recifesaudavel.app.usecase

sealed interface UpcomingAppointmentsResult {
    data object NotImplemented : UpcomingAppointmentsResult
}

/**
 * Asks for the signed-in person's next appointments.
 * The appointment API does not exist yet.
 */
class ListUpcomingAppointments {
    operator fun invoke(): UpcomingAppointmentsResult = UpcomingAppointmentsResult.NotImplemented
}
