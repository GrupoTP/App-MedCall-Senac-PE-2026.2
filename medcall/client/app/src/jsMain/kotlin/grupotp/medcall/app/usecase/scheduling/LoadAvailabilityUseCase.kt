package grupotp.medcall.app.usecase.scheduling

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.gateway.scheduling.AppointmentGateway
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.scheduling.ClinicAvailability

/**
 * Open days and times, always asked to the server (RN-009).
 */
class LoadAvailabilityUseCase(
    private val gateway: AppointmentGateway,
) {
    suspend operator fun invoke(
        account: AuthenticatedAccount,
        specialtyId: Int,
        professionalId: Int?,
    ): RemoteResult<List<ClinicAvailability>> = gateway.availability(account.sessionToken, specialtyId, professionalId)
}
