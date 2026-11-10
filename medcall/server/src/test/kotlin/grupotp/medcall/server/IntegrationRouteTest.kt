package grupotp.medcall.server

import grupotp.medcall.common.account.Phone
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.api.ApiError
import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.auth.RegisterAccountRequest
import grupotp.medcall.common.integration.ApiKeyWithSecret
import grupotp.medcall.common.integration.CreateApiKeyRequest
import grupotp.medcall.common.integration.PublicAppointment
import grupotp.medcall.common.integration.SaveWebhookRequest
import grupotp.medcall.common.integration.WebhookEvent
import grupotp.medcall.common.scheduling.Appointment
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.scheduling.ClinicAvailability
import grupotp.medcall.common.scheduling.CreateAppointmentRequest
import grupotp.medcall.common.scheduling.Specialty
import grupotp.medcall.common.security.SecretString
import grupotp.medcall.common.staff.ChangeStaffPasswordRequest
import grupotp.medcall.common.staff.CreateStaffUserRequest
import grupotp.medcall.common.staff.StaffAccount
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.common.staff.StaffSignInRequest
import grupotp.medcall.common.staff.StaffUserWithPassword
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
import kotlin.test.assertTrue

/** API keys, the public API with idempotency, and webhook URL checks (SPM-97, SPM-99, SPM-100). */
class IntegrationRouteTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aClinicSystemConfirmsByApiKeyOnceEvenWhenItRetries() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        // Arrange: Boa Vista's admin creates a key; a patient books there.
        val admin = boaVistaAdmin()
        val created = client.post("/api/v1/clinic/api-keys") { bearerAuth(admin); jsonBody(CreateApiKeyRequest("ERP de teste")) }
        val key = json.decodeFromString<ApiKeyWithSecret>(created.bodyAsText()).key.value
        val appointment = book(registerPatient(), "Oftalmologia")

        // Act
        val listed = client.get("/api/public/v1/appointments?status=pending") { bearerAuth(key) }
        val first = client.post("/api/public/v1/appointments/${appointment.id}/confirm") { bearerAuth(key); header("Idempotency-Key", "abc-1") }
        val replay = client.post("/api/public/v1/appointments/${appointment.id}/confirm") { bearerAuth(key); header("Idempotency-Key", "abc-1") }
        val again = client.post("/api/public/v1/appointments/${appointment.id}/confirm") { bearerAuth(key) }
        val reused = client.post("/api/public/v1/appointments/${appointment.id}/decline") {
            bearerAuth(key)
            header("Idempotency-Key", "abc-1")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"other"}""")
        }
        val anonymous = client.get("/api/public/v1/appointments")
        val staffToken = client.get("/api/public/v1/appointments") { bearerAuth(admin) }

        // Assert
        assertEquals(HttpStatusCode.Created, created.status)
        assertTrue(key.startsWith("mc_live_"))
        assertTrue(json.decodeFromString<List<PublicAppointment>>(listed.bodyAsText()).any { it.id == appointment.id })
        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(AppointmentStatus.Confirmed, json.decodeFromString<PublicAppointment>(first.bodyAsText()).status)
        assertEquals(HttpStatusCode.OK, replay.status)
        assertEquals("true", replay.headers["Idempotent-Replayed"])
        assertEquals(HttpStatusCode.Conflict, again.status)
        assertEquals(ErrorCode.APPOINTMENT_TRANSITION_INVALID, json.decodeFromString<ApiError>(again.bodyAsText()).code)
        assertEquals(HttpStatusCode.UnprocessableEntity, reused.status)
        assertEquals(HttpStatusCode.Unauthorized, anonymous.status)
        assertEquals(HttpStatusCode.Unauthorized, staffToken.status)
    }

    @Test
    fun revokedKeysStopWorkingAndInternalWebhookUrlsAreRefused() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val admin = boaVistaAdmin()
        val created = json.decodeFromString<ApiKeyWithSecret>(
            client.post("/api/v1/clinic/api-keys") { bearerAuth(admin); jsonBody(CreateApiKeyRequest("Temporária")) }.bodyAsText(),
        )

        val revoked = client.post("/api/v1/clinic/api-keys/${created.apiKey.id}/revoke") { bearerAuth(admin) }
        val afterRevoke = client.get("/api/public/v1/appointments") { bearerAuth(created.key.value) }
        val localhost = client.post("/api/v1/clinic/webhooks") {
            bearerAuth(admin)
            jsonBody(SaveWebhookRequest("https://localhost/hook", events = listOf(WebhookEvent.AppointmentCreated)))
        }
        val plainHttp = client.post("/api/v1/clinic/webhooks") {
            bearerAuth(admin)
            jsonBody(SaveWebhookRequest("http://example.com/hook", events = listOf(WebhookEvent.AppointmentCreated)))
        }
        val privateIp = client.post("/api/v1/clinic/webhooks") {
            bearerAuth(admin)
            jsonBody(SaveWebhookRequest("https://10.0.0.5/hook", events = listOf(WebhookEvent.AppointmentCreated)))
        }

        assertEquals(HttpStatusCode.OK, revoked.status)
        assertEquals(HttpStatusCode.Unauthorized, afterRevoke.status)
        listOf(localhost, plainHttp, privateIp).forEach {
            assertEquals(HttpStatusCode.BadRequest, it.status)
            assertEquals(ErrorCode.WEBHOOK_URL_INVALID, json.decodeFromString<ApiError>(it.bodyAsText()).code)
        }
    }

    // --- helpers ---

    private inline fun <reified T> HttpRequestBuilder.jsonBody(value: T) {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(value))
    }

    private suspend fun ApplicationTestBuilder.signIn(email: String, password: String): String {
        val response = client.post("/api/v1/staff/auth/login") { jsonBody(StaffSignInRequest(email, SecretString(password))) }
        return json.decodeFromString<StaffAccount>(response.bodyAsText()).sessionToken.value
    }

    /** A fresh administrator of Clínica Boa Vista, with the temporary password already changed. */
    private suspend fun ApplicationTestBuilder.boaVistaAdmin(): String {
        val medcall = signIn("admin@medcall.test", "medcall-test-admin")
        val clinics = json.decodeFromString<List<ManagedClinic>>(client.get("/api/v1/admin/clinics") { bearerAuth(medcall) }.bodyAsText())
        val boaVista = clinics.first { it.name == "Clínica Boa Vista" }
        val created = json.decodeFromString<StaffUserWithPassword>(
            client.post("/api/v1/admin/clinics/${boaVista.id}/users") {
                bearerAuth(medcall)
                jsonBody(CreateStaffUserRequest("Admin Integração", "integra.${System.nanoTime()}@clinica.test", StaffRole.ClinicAdmin))
            }.bodyAsText(),
        )
        val token = signIn(created.user.email, created.temporaryPassword.value)
        client.post("/api/v1/staff/auth/password") {
            bearerAuth(token)
            jsonBody(ChangeStaffPasswordRequest(created.temporaryPassword, SecretString("senha-da-integracao")))
        }
        return token
    }

    private suspend fun ApplicationTestBuilder.registerPatient(): AuthenticatedAccount {
        val response = client.post("/api/v1/auth/register") {
            jsonBody(
                RegisterAccountRequest(
                    fullName = "Paciente da Integração",
                    socialName = "",
                    email = "integra.${System.nanoTime()}@email.com",
                    password = SecretString("segredo"),
                    primaryPhone = Phone.parse("(81) 99999-1111"),
                    primaryPhoneWhatsapp = false,
                    secondaryPhone = Phone.parse(""),
                    secondaryPhoneWhatsapp = false,
                    emergencyContactName = "",
                    emergencyContactPhone = Phone.parse(""),
                    acceptedPrivacyPolicy = true,
                    acceptedTreatment = true,
                    wantsNotifications = false,
                ),
            )
        }
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.book(patient: AuthenticatedAccount, specialtyName: String): Appointment {
        val token = patient.sessionToken.value
        val specialty = json.decodeFromString<List<Specialty>>(client.get("/api/v1/specialties") { bearerAuth(token) }.bodyAsText())
            .first { it.name == specialtyName }
        val day = json.decodeFromString<List<ClinicAvailability>>(
            client.get("/api/v1/availability?specialtyId=${specialty.id}") { bearerAuth(token) }.bodyAsText(),
        ).first().days.first()
        val response = client.post("/api/v1/appointments") {
            bearerAuth(token)
            jsonBody(CreateAppointmentRequest(specialty.id, null, date = day.date, time = day.times.first(), reason = "Integração", notes = ""))
        }
        return json.decodeFromString(response.bodyAsText())
    }
}
