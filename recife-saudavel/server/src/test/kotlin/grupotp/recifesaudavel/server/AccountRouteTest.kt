package grupotp.recifesaudavel.server

import grupotp.recifesaudavel.common.AccountProfile
import grupotp.recifesaudavel.common.ApiError
import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.ChangePasswordRequest
import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.common.SignInRequest
import grupotp.recifesaudavel.common.UpdateConsentsRequest
import grupotp.recifesaudavel.common.UpdateProfileRequest
import io.ktor.client.HttpClient
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
import kotlinx.serialization.encodeToString
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
        val response = client.get("/api/v1/account") { bearerAuth(account.sessionToken) }

        // Assert
        assertEquals(HttpStatusCode.Unauthorized, anonymous.status)
        assertEquals(HttpStatusCode.OK, response.status)
        val profile = decode<AccountProfile>(response)
        assertEquals("Maria das Graças Silva", profile.fullName)
        assertEquals("81999999999", profile.primaryPhone)
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
            primaryPhone = "(81) 97777-6666",
            primaryPhoneWhatsapp = false,
            secondaryPhone = "(81) 3333-4444",
            secondaryPhoneWhatsapp = true,
            emergencyContactName = "João",
            emergencyContactPhone = "(81) 95555-4444",
        )

        // Act
        val updated = update(account, edited)
        val taken = update(account, edited.copy(email = other.email))
        val blank = update(account, edited.copy(fullName = " "))
        val reloaded = decode<AccountProfile>(client.get("/api/v1/account") { bearerAuth(account.sessionToken) })

        // Assert
        assertEquals(HttpStatusCode.OK, updated.status)
        assertEquals(edited.email.trim().lowercase(), reloaded.email)
        assertEquals("Maria Silva", reloaded.fullName)
        assertEquals("", reloaded.socialName)
        assertEquals("81977776666", reloaded.primaryPhone)
        assertEquals("8133334444", reloaded.secondaryPhone)
        assertTrue(reloaded.secondaryPhoneWhatsapp)
        assertEquals(HttpStatusCode.BadRequest, taken.status)
        assertEquals("E-mail já cadastrado.", decode<ApiError>(taken).message)
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
        assertEquals("Senha atual incorreta.", decode<ApiError>(wrong).message)
        assertEquals(HttpStatusCode.BadRequest, short.status)
        assertEquals(HttpStatusCode.OK, changed.status)
        assertEquals(HttpStatusCode.Unauthorized, oldLogin.status)
        assertEquals(HttpStatusCode.OK, newLogin.status)
    }

    @Test
    fun optionalConsentAndDeletionRequestArePersisted() = testApplication {
        // Arrange
        useApplicationConfig()
        val account = register("consent.${System.nanoTime()}@email.com")

        // Act
        val consent = client.put("/api/v1/account/consents") {
            bearerAuth(account.sessionToken)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateConsentsRequest(wantsNotifications = true)))
        }
        val first = client.post("/api/v1/account/deletion-request") {
            bearerAuth(account.sessionToken)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val second = client.post("/api/v1/account/deletion-request") {
            bearerAuth(account.sessionToken)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        // Assert
        assertTrue(decode<AccountProfile>(consent).wantsNotifications)
        val firstDate = decode<AccountProfile>(first).deletionRequestedAt
        assertNotNull(firstDate)
        assertEquals(firstDate, decode<AccountProfile>(second).deletionRequestedAt)
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
            setBody(Json.encodeToString(SignInRequest(email = email, password = password)))
        }

    private suspend fun ApplicationTestBuilder.update(account: AuthenticatedAccount, request: UpdateProfileRequest) =
        client.put("/api/v1/account") {
            bearerAuth(account.sessionToken)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }

    private suspend fun ApplicationTestBuilder.changePassword(
        account: AuthenticatedAccount,
        current: String,
        next: String,
    ) = client.post("/api/v1/account/password") {
        bearerAuth(account.sessionToken)
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(ChangePasswordRequest(currentPassword = current, newPassword = next)))
    }

    private suspend inline fun <reified T> decode(response: HttpResponse): T =
        Json { ignoreUnknownKeys = true }.decodeFromString(response.bodyAsText())

    private fun registration(email: String) = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = "Graça",
        email = email,
        password = "segredo",
        primaryPhone = "(81) 99999-9999",
        primaryPhoneWhatsapp = true,
        secondaryPhone = "",
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "Ana Silva",
        emergencyContactPhone = "(81) 98888-7777",
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )
}
