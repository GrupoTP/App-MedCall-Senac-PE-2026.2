package grupotp.recifesaudavel.app.usecase.scheduling

import grupotp.recifesaudavel.app.domain.scheduling.AppointmentGateway
import grupotp.recifesaudavel.app.domain.scheduling.AppointmentTimeline
import grupotp.recifesaudavel.app.domain.scheduling.LocalAppointmentGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.scheduling.Appointment

/**
 * Cancels on the server. Like booking, the device copy only changes after the server accepted it.
 */
class CancelAppointmentUseCase(
    private val gateway: AppointmentGateway,
    private val local: LocalAppointmentGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount, appointmentId: Int): SchedulingResult<Appointment> {
        val result = gateway.cancel(account.sessionToken, appointmentId)
        if (result is SchedulingResult.Success) {
            runCatching {
                val saved = local.loadAppointments(account.id).orEmpty().filter { it.id != result.value.id }
                local.saveAppointments(account.id, AppointmentTimeline.soonestFirst(saved + result.value))
            }
        }
        return result
    }
}
