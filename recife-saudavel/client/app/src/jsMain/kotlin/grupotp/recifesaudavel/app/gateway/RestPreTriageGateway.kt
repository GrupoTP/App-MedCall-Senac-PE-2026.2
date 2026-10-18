package grupotp.recifesaudavel.app.gateway

import grupotp.recifesaudavel.app.domain.pretriage.PreTriageGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.pretriage.PendingPreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.serialization.json.Json

class RestPreTriageGateway(
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : PreTriageGateway {
    override suspend fun draft(token: SecretString): SchedulingResult<PreTriageForm> =
        json.callApi("GET", "$baseUrl/api/v1/pre-triages/draft", token)

    override suspend fun submit(token: SecretString, form: PreTriageForm): SchedulingResult<PreTriage> =
        json.callApi("POST", "$baseUrl/api/v1/pre-triages", token, json.encodeToString(form))

    override suspend fun pending(token: SecretString): SchedulingResult<PreTriage?> =
        when (val result = json.callApi<PendingPreTriage>("GET", "$baseUrl/api/v1/pre-triages/pending", token)) {
            is SchedulingResult.Success -> SchedulingResult.Success(result.value.preTriage)
            is SchedulingResult.Failed -> result
        }
}
