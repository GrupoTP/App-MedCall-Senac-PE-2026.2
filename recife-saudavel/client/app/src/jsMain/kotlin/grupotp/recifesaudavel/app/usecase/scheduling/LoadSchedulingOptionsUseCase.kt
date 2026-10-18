package grupotp.recifesaudavel.app.usecase.scheduling

import grupotp.recifesaudavel.app.domain.scheduling.AppointmentGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingOptions
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

/**
 * Specialties, professionals and their clinics for the first step. Loading them also tells
 * whether the server can be reached, so the flow never opens offline.
 */
class LoadSchedulingOptionsUseCase(
    private val gateway: AppointmentGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): SchedulingResult<SchedulingOptions> {
        val specialties = when (val result = gateway.specialties(account.sessionToken)) {
            is SchedulingResult.Success -> result.value
            is SchedulingResult.Failed -> return result
        }
        val professionals = when (val result = gateway.professionals(account.sessionToken)) {
            is SchedulingResult.Success -> result.value
            is SchedulingResult.Failed -> return result
        }
        return when (val result = gateway.clinics(account.sessionToken)) {
            is SchedulingResult.Success -> SchedulingResult.Success(SchedulingOptions(specialties, professionals, result.value))
            is SchedulingResult.Failed -> result
        }
    }
}
