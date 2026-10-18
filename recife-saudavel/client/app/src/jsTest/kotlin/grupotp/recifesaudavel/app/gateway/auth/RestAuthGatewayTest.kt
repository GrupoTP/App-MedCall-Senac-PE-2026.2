package grupotp.recifesaudavel.app.gateway.auth

import grupotp.recifesaudavel.app.domain.auth.AuthError
import grupotp.recifesaudavel.app.domain.auth.AuthResult
import grupotp.recifesaudavel.app.gateway.http.apiHttpClient
import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.common.api.ErrorCode
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestAuthGatewayTest {
    private val account = AuthenticatedAccount(1, "Polyana Fontes", "Poly", "poly@email.com", SecretString("token-123"))
    private val signIn = SignInRequest("poly@email.com", SecretString("senha-123"))
    private val requests = mutableListOf<HttpRequestData>()

    private fun gateway(
        answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = RestAuthGateway(
        "http://api.test",
        apiHttpClient(MockEngine { request -> requests += request; answer(request) }),
    )

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun signInSendsTheRequestAsJsonWithoutToken() = runTest {
        val result = gateway { json(HttpStatusCode.OK, Json.encodeToString(account)) }.signIn(signIn)

        assertEquals(AuthResult.Success(account), result)
        val request = requests.single()
        assertTrue(request.body.contentType?.match(ContentType.Application.Json) == true)
        assertEquals(signIn, Json.decodeFromString<SignInRequest>(request.body.toByteArray().decodeToString()))
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun unauthorizedSignInCarriesTheServerMessage() = runTest {
        val error = ApiError(ErrorCode.WRONG_CREDENTIALS, "E-mail ou senha incorretos.")

        val result = gateway { json(HttpStatusCode.Unauthorized, Json.encodeToString(error)) }.signIn(signIn)

        assertEquals(AuthResult.Failed(AuthError.Rejected("E-mail ou senha incorretos.")), result)
    }

    @Test
    fun unauthorizedProfileMeansTheSessionExpired() = runTest {
        val result = gateway { json(HttpStatusCode.Unauthorized, "") }.profile(SecretString("token-123"))

        assertEquals(AuthResult.Failed(AuthError.SessionExpired), result)
    }
}
