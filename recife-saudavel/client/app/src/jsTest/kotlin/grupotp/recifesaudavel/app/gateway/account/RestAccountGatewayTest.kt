package grupotp.recifesaudavel.app.gateway.account

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.http.apiHttpClient
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.common.api.ErrorCode
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RestAccountGatewayTest {
    private val token = SecretString("token-123")
    private val profile = AccountProfile(
        fullName = "Polyana Fontes",
        socialName = "Poly",
        email = "poly@email.com",
        primaryPhone = Phone.parse("81988887777"),
        primaryPhoneWhatsapp = true,
        secondaryPhone = Phone.parse(""),
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = Phone.parse(""),
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
    private val requests = mutableListOf<HttpRequestData>()

    private fun gateway(
        answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = RestAccountGateway(
        "http://api.test",
        apiHttpClient(MockEngine { request -> requests += request; answer(request) }),
    )

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun profileDecodesTheAnswerAndSendsTheToken() = runTest {
        val result = gateway { json(HttpStatusCode.OK, Json.encodeToString(profile)) }.profile(token)

        assertEquals(RemoteResult.Success(profile), result)
        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("http://api.test/api/v1/account", request.url.toString())
        assertEquals("Bearer token-123", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun changePasswordSendsTheRequestAsJson() = runTest {
        val change = ChangePasswordRequest(SecretString("velha"), SecretString("nova-senha"), signOutOtherSessions = true)

        gateway { json(HttpStatusCode.OK, Json.encodeToString(profile)) }.changePassword(token, change)

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(change, Json.decodeFromString<ChangePasswordRequest>(request.body.toByteArray().decodeToString()))
    }

    @Test
    fun unauthorizedMeansTheSessionExpired() = runTest {
        val result = gateway { json(HttpStatusCode.Unauthorized, "") }.profile(token)

        assertEquals(RemoteResult.Failed(RemoteError.SessionExpired), result)
    }

    @Test
    fun clientErrorCarriesTheServerMessage() = runTest {
        val error = ApiError(ErrorCode.WRONG_CREDENTIALS, "Senha atual incorreta.")

        val result = gateway { json(HttpStatusCode.BadRequest, Json.encodeToString(error)) }
            .changePassword(token, ChangePasswordRequest(SecretString("x"), SecretString("y")))

        assertEquals(RemoteResult.Failed(RemoteError.Rejected("Senha atual incorreta.")), result)
    }

    @Test
    fun errorWithoutApiErrorBodyFallsBackToAGenericMessage() = runTest {
        val result = gateway { respond("Bad gateway", HttpStatusCode.BadGateway) }.profile(token)

        assertEquals(RemoteResult.Failed(RemoteError.Rejected("Não foi possível concluir o pedido.")), result)
    }

    @Test
    fun networkFailureMeansOffline() = runTest {
        val result = gateway { throw IOException("connection refused") }.profile(token)

        assertEquals(RemoteResult.Failed(RemoteError.Offline), result)
    }

    @Test
    fun signOutIgnoresTheAnswerBody() = runTest {
        val result = gateway { json(HttpStatusCode.OK, "{\"ok\":true}") }.signOut(token)

        assertEquals(RemoteResult.Success(Unit), result)
        assertEquals("http://api.test/api/v1/auth/logout", requests.single().url.toString())
    }
}
