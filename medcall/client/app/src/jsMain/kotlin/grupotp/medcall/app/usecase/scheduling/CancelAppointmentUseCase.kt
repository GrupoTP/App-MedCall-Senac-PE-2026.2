package grupotp.medcall.app.usecase.scheduling

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.domain.scheduling.AppointmentTimeline
import grupotp.medcall.app.gateway.local.LocalAppointmentGateway
import grupotp.medcall.app.gateway.scheduling.AppointmentGateway
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.scheduling.Appointment

/**
 * Cancels on the server. Like booking, the device copy only changes after the server accepted it.
 */
class CancelAppointmentUseCase(
    private val gateway: AppointmentGateway,
    private val local: LocalAppointmentGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount, appointmentId: Int): RemoteResult<Appointment> {
        val result = gateway.cancel(account.sessionToken, appointmentId)
        if (result is RemoteResult.Success) {
            runCatching {
                val saved = local.loadAppointments(account.id).orEmpty().filter { it.id != result.value.id }
                local.saveAppointments(account.id, AppointmentTimeline.soonestFirst(saved + result.value))
            }
        }
        return result
    }
}
