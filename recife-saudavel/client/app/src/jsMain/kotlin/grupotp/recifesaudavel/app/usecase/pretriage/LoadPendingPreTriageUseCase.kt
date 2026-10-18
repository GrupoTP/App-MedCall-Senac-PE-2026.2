package grupotp.recifesaudavel.app.usecase.pretriage

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.pretriage.PreTriageGateway
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
        (gateway.pending(account.sessionToken) as? RemoteResult.Success)?.value
}
