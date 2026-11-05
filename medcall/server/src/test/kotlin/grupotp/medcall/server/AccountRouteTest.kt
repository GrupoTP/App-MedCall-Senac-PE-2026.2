package grupotp.recifesaudavel.server

import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.api.ApiError
import grupotp.recifesaudavel.common.api.ErrorCode
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.auth.SignInRequest
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountRouteTest {
    @Test
    fun profileNeedsASessionAndReturnsWhatWasRegistered() = testApplication {
        // Arrange
        useApplicationConfig()
        val account = register("perfil.${System.nanoTime()}@email.com")

        // Act
        val anonymous = client.get("/api/v1/account")
        val response = client.get("/api/v1/account") { bearerAuth(account.sessionToken.value) }

        // Assert
        assertEquals(HttpStatusCode.Unauthorized, anonymous.status)
        assertEquals(HttpStatusCode.OK, response.status)
        val profile = decode<AccountProfile>(response)
        assertEquals("Maria das Graças Silva", profile.fullName)
        assertEquals("81999999999", profile.primaryPhone.digits)
        assertTrue(profile.primaryPhoneWhatsapp)
        assertTrue(profile.acceptedPrivacyPolicy)
        assertFalse(profile.wantsNotifications)
        assertNotNull(profile.createdAt)
        assertNotNull(profile.lastLoginAt)
        assertNull(profile.deletionRequestedAt)
    }

    @Test
    fun updateChangesTheProfileAndRefusesAnEmailOfAnotherAccount() = testApplication {
        // Arrange
        useApplicationConfig()
        val other = register("outra.${System.nanoTime()}@email.com")
        val account = register("editar.${System.nanoTime()}@email.com")
        val edited = UpdateProfileRequest(
            fullName = "Maria Silva",
            socialName = "",
            email = " Novo.${System.nanoTime()}@Email.com ",
            primaryPhone = Phone.parse("(81) 97777-6666"),
            primaryPhoneWhatsapp = false,
            secondaryPhone = Phone.parse("(81) 3333-4444"),
            secondaryPhoneWhatsapp = true,
            emergencyContactName = "João",
            emergencyContactPhone = Phone.parse("(81) 95555-4444"),
        )

        // Act
        val updated = update(account, edited)
        val taken = update(account, edited.copy(email = other.email))
        val blank = update(account, edited.copy(fullName = " "))
        val reloaded = decode<AccountProfile>(client.get("/api/v1/account") { bearerAuth(account.sessionToken.value) })

        // Assert
        assertEquals(HttpStatusCode.OK, updated.status)
        assertEquals(edited.email.trim().lowercase(), reloaded.email)
        assertEquals("Maria Silva", reloaded.fullName)
        assertEquals("", reloaded.socialName)
        assertEquals("81977776666", reloaded.primaryPhone.digits)
        assertEquals("8133334444", reloaded.secondaryPhone.digits)
        assertTrue(reloaded.secondaryPhoneWhatsapp)
        assertEquals(HttpStatusCode.BadRequest, taken.status)
        assertEquals(ApiError(ErrorCode.EMAIL_TAKEN, "E-mail já cadastrado."), decode<ApiError>(taken))
        assertEquals(HttpStatusCode.BadRequest, blank.status)
    }

    @Test
    fun changingThePasswordNeedsTheCurrentOneAndTheNewOneWorksAtSignIn() = testApplication {
        // Arrange
        useApplicationConfig()
        val email = "senha.${System.nanoTime()}@email.com"
        val account = register(email)

        // Act
        val wrong = changePassword(account, "errada1", "novasenha")
        val short = changePassword(account, "segredo", "123")
        val changed = changePassword(account, "segredo", "novasenha")
        val oldLogin = signIn(email, "segredo")
        val newLogin = signIn(email, "novasenha")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, wrong.status)
        assertEquals(ApiError(ErrorCode.CURRENT_PASSWORD_WRONG, "Senha atual incorreta."), decode<ApiError>(wrong))
        assertEquals(HttpStatusCode.BadRequest, short.status)
        assertEquals(ApiError(ErrorCode.PASSWORD_TOO_SHORT, "Mínimo 6 caracteres."), decode<ApiError>(short))
        assertEquals(HttpStatusCode.OK, changed.status)
        assertEquals(HttpStatusCode.Unauthorized, oldLogin.status)
        assertEquals(HttpStatusCode.OK, newLogin.status)
    }

    @Test
    fun optionalConsentAndDeletionRequestArePersisted() = testApplication {
        // Arrange
        useApplicationConfig()
        val email = "consent.${System.nanoTime()}@email.com"
        val account = register(email)
        val otherDevice = decode<AuthenticatedAccount>(signIn(email, "segredo"))

        // Act
        val consent = client.put("/api/v1/account/consents") {
            bearerAuth(account.sessionToken.value)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateConsentsRequest(wantsNotifications = true)))
        }
        val first = requestDeletion(account)
        val sameSession = client.get("/api/v1/account") { bearerAuth(account.sessionToken.value) }
        val otherSession = client.get("/api/v1/account") { bearerAuth(otherDevice.sessionToken.value) }
        val again = decode<AuthenticatedAccount>(signIn(email, "segredo"))
        val second = requestDeletion(again)

        // Assert
        assertTrue(decode<AccountProfile>(consent).wantsNotifications)
        val firstDate = decode<AccountProfile>(first).deletionRequestedAt
        assertNotNull(firstDate)
        assertEquals(HttpStatusCode.Unauthorized, sameSession.status)
        assertEquals(HttpStatusCode.Unauthorized, otherSession.status)
        assertEquals(firstDate, decode<AccountProfile>(second).deletionRequestedAt)
    }

    @Test
    fun changingThePasswordCanEndTheOtherSessionsOnly() = testApplication {
        // Arrange
        useApplicationConfig()
        val email = "sessoes.${System.nanoTime()}@email.com"
        val account = register(email)
        val otherDevice = decode<AuthenticatedAccount>(signIn(email, "segredo"))

        // Act
        val kept = changePassword(account, "segredo", "novasenha")
        val otherAfterKeeping = client.get("/api/v1/account") { bearerAuth(otherDevice.sessionToken.value) }
        val ended = changePassword(account, "novasenha", "outrasenha", signOutOtherSessions = true)
        val otherAfterEnding = client.get("/api/v1/account") { bearerAuth(otherDevice.sessionToken.value) }
        val thisAfterEnding = client.get("/api/v1/account") { bearerAuth(account.sessionToken.value) }

        // Assert
        assertEquals(HttpStatusCode.OK, kept.status)
        assertEquals(HttpStatusCode.OK, otherAfterKeeping.status)
        assertEquals(HttpStatusCode.OK, ended.status)
        assertEquals(HttpStatusCode.Unauthorized, otherAfterEnding.status)
        assertEquals(HttpStatusCode.OK, thisAfterEnding.status)
    }

    @Test
    fun logoutRevokesTheToken() = testApplication {
        // Arrange
        useApplicationConfig()
        val account = register("sair.${System.nanoTime()}@email.com")

        // Act
        val before = client.get("/api/v1/account") { bearerAuth(account.sessionToken.value) }
        val logout = client.post("/api/v1/auth/logout") {
            bearerAuth(account.sessionToken.value)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val after = client.get("/api/v1/account") { bearerAuth(account.sessionToken.value) }

        // Assert
        assertEquals(HttpStatusCode.OK, before.status)
        assertEquals(HttpStatusCode.OK, logout.status)
        assertEquals(HttpStatusCode.Unauthorized, after.status)
    }

    private fun ApplicationTestBuilder.useApplicationConfig() {
        environment {
            config = ApplicationConfig("application.yaml")
        }
    }

    private suspend fun ApplicationTestBuilder.register(email: String): AuthenticatedAccount {
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(registration(email)))
        }
        return decode(response)
    }

    private suspend fun ApplicationTestBuilder.signIn(email: String, password: String): HttpResponse =
        client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(SignInRequest(email = email, password = SecretString(password))))
        }

    private suspend fun ApplicationTestBuilder.update(account: AuthenticatedAccount, request: UpdateProfileRequest) =
        client.put("/api/v1/account") {
            bearerAuth(account.sessionToken.value)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

    private suspend fun ApplicationTestBuilder.changePassword(
        account: AuthenticatedAccount,
        current: String,
        next: String,
        signOutOtherSessions: Boolean = false,
    ) = client.post("/api/v1/account/password") {
        bearerAuth(account.sessionToken.value)
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(ChangePasswordRequest(SecretString(current), SecretString(next), signOutOtherSessions)))
    }

    private suspend fun ApplicationTestBuilder.requestDeletion(account: AuthenticatedAccount) =
        client.post("/api/v1/account/deletion-request") {
            bearerAuth(account.sessionToken.value)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

    private suspend inline fun <reified T> decode(response: HttpResponse): T =
        Json { ignoreUnknownKeys = true }.decodeFromString(response.bodyAsText())

    private fun registration(email: String) = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = "Graça",
        email = email,
        password = SecretString("segredo"),
        primaryPhone = Phone.parse("(81) 99999-9999"),
        primaryPhoneWhatsapp = true,
        secondaryPhone = Phone.parse(""),
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "Ana Silva",
        emergencyContactPhone = Phone.parse("(81) 98888-7777"),
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
}
