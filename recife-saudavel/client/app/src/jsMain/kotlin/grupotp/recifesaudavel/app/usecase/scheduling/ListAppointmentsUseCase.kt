package grupotp.recifesaudavel.app.usecase.scheduling

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.scheduling.AppointmentTimeline
import grupotp.recifesaudavel.app.domain.scheduling.AppointmentsSnapshot
import grupotp.recifesaudavel.app.gateway.local.LocalAppointmentGateway
import grupotp.recifesaudavel.app.gateway.scheduling.AppointmentGateway
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
            is RemoteResult.Success -> {
                val appointments = AppointmentTimeline.soonestFirst(result.value)
                runCatching { local.saveAppointments(account.id, appointments) }
                AppointmentsSnapshot(appointments, offline = false)
            }
            is RemoteResult.Failed -> AppointmentsSnapshot(
                appointments = runCatching { local.loadAppointments(account.id) }.getOrNull().orEmpty(),
                offline = result.error == RemoteError.Offline,
                error = result.error.takeUnless { it == RemoteError.Offline },
            )
        }
}
