package grupotp.recifesaudavel.app.usecase.scheduling

import grupotp.recifesaudavel.app.domain.scheduling.AppointmentGateway
import grupotp.recifesaudavel.app.domain.scheduling.AppointmentTimeline
import grupotp.recifesaudavel.app.domain.scheduling.AppointmentsSnapshot
import grupotp.recifesaudavel.app.domain.scheduling.LocalAppointmentGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingError
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

/**
 * Server first, device copy when offline. Each fresh list replaces the device copy.
 */
class ListAppointmentsUseCase(
    private val gateway: AppointmentGateway,
    private val local: LocalAppointmentGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): AppointmentsSnapshot =
        when (val result = gateway.list(account.sessionToken)) {
            is SchedulingResult.Success -> {
                val appointments = AppointmentTimeline.soonestFirst(result.value)
                runCatching { local.saveAppointments(account.id, appointments) }
                AppointmentsSnapshot(appointments, offline = false)
            }
            is SchedulingResult.Failed -> AppointmentsSnapshot(
                appointments = runCatching { local.loadAppointments(account.id) }.getOrNull().orEmpty(),
                offline = result.error == SchedulingError.Offline,
                error = result.error.takeUnless { it == SchedulingError.Offline },
            )
        }
}
