package grupotp.medcall.app.gateway.pretriage

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.gateway.http.callApi
import grupotp.medcall.common.pretriage.PendingPreTriage
import grupotp.medcall.common.pretriage.PreTriage
import grupotp.medcall.common.pretriage.PreTriageForm
import grupotp.medcall.common.security.SecretString
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
