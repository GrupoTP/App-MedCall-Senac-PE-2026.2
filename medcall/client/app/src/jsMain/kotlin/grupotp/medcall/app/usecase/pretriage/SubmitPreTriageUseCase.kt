package grupotp.medcall.app.usecase.pretriage

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.gateway.pretriage.PreTriageGateway
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.pretriage.PreTriage
import grupotp.medcall.common.pretriage.PreTriageForm

/**
 * Sends the pre-triage. The profile is never changed by it, even when the personal data differ.
 */
class SubmitPreTriageUseCase(
    private val gateway: PreTriageGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount, form: PreTriageForm): RemoteResult<PreTriage> =
        gateway.submit(account.sessionToken, form)
}
