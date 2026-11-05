package grupotp.medcall.app.usecase.pretriage

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.gateway.pretriage.PreTriageGateway
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.pretriage.PreTriageForm

/**
 * Form to start from: personal data from the current profile, medical history and medications
 * from the last pre-triage. Loading it also tells whether the server can be reached.
 */
class LoadPreTriageDraftUseCase(
    private val gateway: PreTriageGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): RemoteResult<PreTriageForm> =
        gateway.draft(account.sessionToken)
}
