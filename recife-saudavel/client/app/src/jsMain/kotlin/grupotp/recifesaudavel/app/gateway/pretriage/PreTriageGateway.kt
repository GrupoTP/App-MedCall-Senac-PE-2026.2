package grupotp.recifesaudavel.app.gateway.pretriage

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Pre-triage API of the server. Every call needs the session token.
 */
interface PreTriageGateway {
    suspend fun draft(token: SecretString): RemoteResult<PreTriageForm>

    suspend fun submit(token: SecretString, form: PreTriageForm): RemoteResult<PreTriage>

    suspend fun pending(token: SecretString): RemoteResult<PreTriage?>
}
