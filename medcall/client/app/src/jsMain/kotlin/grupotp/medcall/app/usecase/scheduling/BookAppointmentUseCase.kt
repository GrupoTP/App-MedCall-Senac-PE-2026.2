package grupotp.medcall.app.usecase.scheduling

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.domain.scheduling.AppointmentTimeline
import grupotp.medcall.app.gateway.local.LocalAppointmentGateway
import grupotp.medcall.app.gateway.scheduling.AppointmentGateway
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.scheduling.Appointment
import grupotp.medcall.common.scheduling.CreateAppointmentRequest

/**
 * Books on the server. The local copy only changes after the server stored the appointment, so a
 * failure never shows up as a booked appointment (RNF-012).
 */
class BookAppointmentUseCase(
    private val gateway: AppointmentGateway,
    private val local: LocalAppointmentGateway,
) {
    suspend operator fun invoke(
        account: AuthenticatedAccount,
        request: CreateAppointmentRequest,
    ): RemoteResult<Appointment> {
        val result = gateway.book(account.sessionToken, request)
        if (result is RemoteResult.Success) {
            runCatching {
                val saved = local.loadAppointments(account.id).orEmpty().filter { it.id != result.value.id }
                local.saveAppointments(account.id, AppointmentTimeline.soonestFirst(saved + result.value))
            }
        }
        return result
    }
}
