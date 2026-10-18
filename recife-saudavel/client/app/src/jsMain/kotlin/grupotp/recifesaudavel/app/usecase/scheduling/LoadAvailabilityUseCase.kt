package grupotp.recifesaudavel.app.usecase.scheduling

import grupotp.recifesaudavel.app.domain.scheduling.AppointmentGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability

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
    ): SchedulingResult<List<ClinicAvailability>> = gateway.availability(account.sessionToken, specialtyId, professionalId)
}
