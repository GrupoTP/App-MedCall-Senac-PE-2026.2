package grupotp.recifesaudavel.app.usecase.pretriage

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.pretriage.PreTriageGateway
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm

/**
 * Sends the pre-triage. The profile is never changed by it, even when the personal data differ.
 */
class SubmitPreTriageUseCase(
    private val gateway: PreTriageGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount, form: PreTriageForm): RemoteResult<PreTriage> =
        gateway.submit(account.sessionToken, form)
}
