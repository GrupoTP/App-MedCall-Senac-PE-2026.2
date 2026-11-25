package grupotp.medcall.app.gateway.auth

import grupotp.medcall.app.domain.auth.AuthError
import grupotp.medcall.app.domain.auth.AuthResult
import grupotp.medcall.app.domain.auth.RecoveryResult
import grupotp.medcall.common.auth.Acknowledged
import grupotp.medcall.common.auth.PasswordRecoveryRequest
import grupotp.medcall.app.gateway.http.apiHttpClient
import grupotp.medcall.common.api.ApiError
import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.auth.SignInRequest
import grupotp.medcall.common.security.SecretString
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
    fun passwordRecoveryPostsTheEmailAndReadsTheAcknowledgement() = runTest {
        val result = gateway { json(HttpStatusCode.OK, Json.encodeToString(Acknowledged())) }
            .requestPasswordRecovery("poly@email.com")

        assertEquals(RecoveryResult.Accepted, result)
        val request = requests.single()
        assertEquals(
            PasswordRecoveryRequest("poly@email.com"),
            Json.decodeFromString<PasswordRecoveryRequest>(request.body.toByteArray().decodeToString()),
        )
        assertEquals("/api/v1/auth/password-recovery", request.url.encodedPath)
    }

    @Test
    fun passwordRecoveryKeepsTheServerMessage() = runTest {
        val error = ApiError(ErrorCode.MAIL_UNAVAILABLE, "O envio do código de recuperação não está disponível neste servidor.")

        val result = gateway { json(HttpStatusCode.ServiceUnavailable, Json.encodeToString(error)) }
            .requestPasswordRecovery("poly@email.com")

        assertEquals(RecoveryResult.Failed(AuthError.Rejected(error.message)), result)
    }

    @Test
    fun unauthorizedProfileMeansTheSessionExpired() = runTest {
        val result = gateway { json(HttpStatusCode.Unauthorized, "") }.profile(SecretString("token-123"))

        assertEquals(AuthResult.Failed(AuthError.SessionExpired), result)
    }
}
