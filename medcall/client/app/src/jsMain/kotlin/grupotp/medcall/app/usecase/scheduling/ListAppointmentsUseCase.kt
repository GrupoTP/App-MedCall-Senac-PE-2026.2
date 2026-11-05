package grupotp.medcall.app.usecase.scheduling

import grupotp.medcall.app.domain.RemoteError
import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.domain.scheduling.AppointmentTimeline
import grupotp.medcall.app.domain.scheduling.AppointmentsSnapshot
import grupotp.medcall.app.gateway.local.LocalAppointmentGateway
import grupotp.medcall.app.gateway.scheduling.AppointmentGateway
import grupotp.medcall.common.auth.AuthenticatedAccount

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
