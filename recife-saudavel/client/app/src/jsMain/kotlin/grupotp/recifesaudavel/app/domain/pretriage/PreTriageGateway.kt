package grupotp.recifesaudavel.app.domain.pretriage

import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Pre-triage API of the server. Every call needs the session token.
 */
interface PreTriageGateway {
    suspend fun draft(token: SecretString): SchedulingResult<PreTriageForm>

    suspend fun submit(token: SecretString, form: PreTriageForm): SchedulingResult<PreTriage>

    suspend fun pending(token: SecretString): SchedulingResult<PreTriage?>
}
