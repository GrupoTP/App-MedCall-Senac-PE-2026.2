package grupotp.recifesaudavel.app.usecase.scheduling

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingOptions
import grupotp.recifesaudavel.app.gateway.scheduling.AppointmentGateway
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

/**
 * Specialties, professionals and their clinics for the first step. Loading them also tells
 * whether the server can be reached, so the flow never opens offline.
 */
class LoadSchedulingOptionsUseCase(
    private val gateway: AppointmentGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): RemoteResult<SchedulingOptions> {
        val specialties = when (val result = gateway.specialties(account.sessionToken)) {
            is RemoteResult.Success -> result.value
            is RemoteResult.Failed -> return result
        }
        val professionals = when (val result = gateway.professionals(account.sessionToken)) {
            is RemoteResult.Success -> result.value
            is RemoteResult.Failed -> return result
        }
        return when (val result = gateway.clinics(account.sessionToken)) {
            is RemoteResult.Success -> RemoteResult.Success(SchedulingOptions(specialties, professionals, result.value))
            is RemoteResult.Failed -> result
        }
    }
}
