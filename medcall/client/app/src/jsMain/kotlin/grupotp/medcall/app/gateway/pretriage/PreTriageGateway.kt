package grupotp.medcall.app.gateway.pretriage

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.common.pretriage.PreTriage
import grupotp.medcall.common.pretriage.PreTriageForm
import grupotp.medcall.common.security.SecretString

/**
 * Pre-triage API of the server. Every call needs the session token.
 */
interface PreTriageGateway {
    suspend fun draft(token: SecretString): RemoteResult<PreTriageForm>

    suspend fun submit(token: SecretString, form: PreTriageForm): RemoteResult<PreTriage>

    suspend fun pending(token: SecretString): RemoteResult<PreTriage?>
}
