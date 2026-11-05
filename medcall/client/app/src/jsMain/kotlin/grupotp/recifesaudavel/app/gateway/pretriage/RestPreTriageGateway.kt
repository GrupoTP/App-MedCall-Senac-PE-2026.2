package grupotp.recifesaudavel.app.gateway.pretriage

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.http.callApi
import grupotp.recifesaudavel.common.pretriage.PendingPreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod

class RestPreTriageGateway(
    private val baseUrl: String,
    private val http: HttpClient,
) : PreTriageGateway {
    override suspend fun draft(token: SecretString): RemoteResult<PreTriageForm> =
        http.callApi(HttpMethod.Get, "$baseUrl/api/v1/pre-triages/draft", token)

    override suspend fun submit(token: SecretString, form: PreTriageForm): RemoteResult<PreTriage> =
        http.callApi(HttpMethod.Post, "$baseUrl/api/v1/pre-triages", token) { setBody(form) }

    override suspend fun pending(token: SecretString): RemoteResult<PreTriage?> =
        when (val result = http.callApi<PendingPreTriage>(HttpMethod.Get, "$baseUrl/api/v1/pre-triages/pending", token)) {
            is RemoteResult.Success -> RemoteResult.Success(result.value.preTriage)
            is RemoteResult.Failed -> result
        }
}
