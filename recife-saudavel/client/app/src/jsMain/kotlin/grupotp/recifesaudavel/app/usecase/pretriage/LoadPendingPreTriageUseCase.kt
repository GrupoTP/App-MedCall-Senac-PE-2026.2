package grupotp.recifesaudavel.app.usecase.pretriage

import grupotp.recifesaudavel.app.domain.pretriage.PreTriageGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.pretriage.PreTriage

/**
 * Latest pre-triage the next booking may attach, or null. A failure counts as none, so the
 * booking never depends on it.
 */
class LoadPendingPreTriageUseCase(
    private val gateway: PreTriageGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): PreTriage? =
        (gateway.pending(account.sessionToken) as? SchedulingResult.Success)?.value
}
