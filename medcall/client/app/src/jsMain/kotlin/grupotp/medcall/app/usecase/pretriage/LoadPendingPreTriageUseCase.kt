package grupotp.medcall.app.usecase.pretriage

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.gateway.pretriage.PreTriageGateway
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.pretriage.PreTriage

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
