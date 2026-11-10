package grupotp.medcall.server

import grupotp.medcall.common.account.Phone
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.admin.SaveClinicRequest
import grupotp.medcall.common.api.ApiError
import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.auth.RegisterAccountRequest
import grupotp.medcall.common.clinic.ClinicAppointment
import grupotp.medcall.common.clinic.ClinicCatalog
import grupotp.medcall.common.clinic.DeclineReason
import grupotp.medcall.common.clinic.DeclineRequest
import grupotp.medcall.common.kpi.ClinicDashboard
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
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.common.staff.StaffUserWithPassword
import grupotp.medcall.common.staff.UpdateStaffUserRequest
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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The clinic portal and the MedCall panel: roles, clinic scope, agenda actions and the rule that
 * the panel never returns patient data. Shares the in-memory database of the other route tests.
 */
class StaffPortalRouteTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun rolesAndTemporaryPasswordsAreEnforced() = staffTest {
        // Arrange
        val medcall = signIn(ADMIN_EMAIL, ADMIN_PASSWORD)
        val clinic = newClinic(medcall)
        val (admin, adminPassword) = createUser(medcall, "/api/v1/admin/clinics/${clinic.id}/users", StaffRole.ClinicAdmin)
        val patient = registerPatient()

        // Act
        val medcallOnClinicRoute = client.get("/api/v1/clinic/catalog") { bearerAuth(medcall) }
        val patientOnClinicRoute = client.get("/api/v1/clinic/catalog") { bearerAuth(patient.sessionToken.value) }
        val adminToken = signIn(admin.email, adminPassword.value)
        val beforeChange = client.get("/api/v1/clinic/catalog") { bearerAuth(adminToken) }
        val changed = client.post("/api/v1/staff/auth/password") {
            bearerAuth(adminToken)
            jsonBody(ChangeStaffPasswordRequest(adminPassword, SecretString("nova-senha-segura")))
        }
        val afterChange = client.get("/api/v1/clinic/catalog") { bearerAuth(adminToken) }
        val clinicOnAdminRoute = client.get("/api/v1/admin/clinics") { bearerAuth(adminToken) }
        val (receptionist, receptionistPassword) = createUser(adminToken, "/api/v1/clinic/users", StaffRole.Receptionist)
        val receptionistToken = signIn(receptionist.email, receptionistPassword.value)
        client.post("/api/v1/staff/auth/password") {
            bearerAuth(receptionistToken)
            jsonBody(ChangeStaffPasswordRequest(receptionistPassword, SecretString("outra-senha-boa")))
        }
        val receptionistOnKpis = client.get("/api/v1/clinic/kpis") { bearerAuth(receptionistToken) }
        val receptionistOnAgenda = client.get("/api/v1/clinic/appointments") { bearerAuth(receptionistToken) }

        // Assert
        assertEquals(HttpStatusCode.Forbidden, medcallOnClinicRoute.status)
        assertEquals(HttpStatusCode.Unauthorized, patientOnClinicRoute.status)
        assertEquals(HttpStatusCode.Forbidden, beforeChange.status)
        assertEquals(ErrorCode.PASSWORD_CHANGE_REQUIRED, error(beforeChange).code)
        assertEquals(HttpStatusCode.OK, changed.status)
        assertEquals(clinic.id, json.decodeFromString<ClinicCatalog>(afterChange.bodyAsText()).clinic.id)
        assertEquals(HttpStatusCode.Forbidden, clinicOnAdminRoute.status)
        assertEquals(HttpStatusCode.Forbidden, receptionistOnKpis.status)
        assertEquals(HttpStatusCode.OK, receptionistOnAgenda.status)
    }

    @Test
    fun theLastAdministratorCannotBeRemoved() = staffTest {
        // Arrange
        val medcall = signIn(ADMIN_EMAIL, ADMIN_PASSWORD)
        val clinic = newClinic(medcall)
        val adminToken = activeUser(medcall, clinic, StaffRole.ClinicAdmin)
        val me = json.decodeFromString<StaffAccount>(client.get("/api/v1/staff/auth/me") { bearerAuth(adminToken) }.bodyAsText())
        val (other, _) = createUser(adminToken, "/api/v1/clinic/users", StaffRole.ClinicAdmin)

        // Act
        val selfDemotion = client.put("/api/v1/clinic/users/${me.id}") {
            bearerAuth(adminToken)
            jsonBody(UpdateStaffUserRequest(role = StaffRole.Receptionist))
        }
        val otherDeactivated = client.put("/api/v1/clinic/users/${other.id}") {
            bearerAuth(adminToken)
            jsonBody(UpdateStaffUserRequest(active = false))
        }

        // Assert
        assertEquals(ErrorCode.CANNOT_CHANGE_SELF, error(selfDemotion).code)
        assertEquals(HttpStatusCode.OK, otherDeactivated.status)
        assertFalse(json.decodeFromString<StaffUser>(otherDeactivated.bodyAsText()).active)
    }

    @Test
    fun theClinicConfirmsAndDeclinesAndThePatientSeesIt() = staffTest {
        // Arrange: a patient books at Clínica Boa Vista, whose staff answers.
        val medcall = signIn(ADMIN_EMAIL, ADMIN_PASSWORD)
        val clinics = json.decodeFromString<List<ManagedClinic>>(client.get("/api/v1/admin/clinics") { bearerAuth(medcall) }.bodyAsText())
        val boaVista = clinics.first { it.name == "Clínica Boa Vista" }
        val reception = activeUser(medcall, boaVista, StaffRole.ClinicAdmin)
        val otherClinic = activeUser(medcall, newClinic(medcall), StaffRole.ClinicAdmin)
        val patient = registerPatient()
        val first = book(patient, "Neurologia", 0)
        val second = book(patient, "Neurologia", 1)

        // Act
        val confirmed = client.post("/api/v1/clinic/appointments/${first.id}/confirm") { bearerAuth(reception) }
        val confirmedAgain = client.post("/api/v1/clinic/appointments/${first.id}/confirm") { bearerAuth(reception) }
        val fromOtherClinic = client.post("/api/v1/clinic/appointments/${second.id}/confirm") { bearerAuth(otherClinic) }
        val declined = client.post("/api/v1/clinic/appointments/${second.id}/decline") {
            bearerAuth(reception)
            jsonBody(DeclineRequest(DeclineReason.ProfessionalUnavailable, "Médico em congresso"))
        }
        val seenByPatient = json.decodeFromString<List<Appointment>>(
            client.get("/api/v1/appointments") { bearerAuth(patient.sessionToken.value) }.bodyAsText(),
        ).associateBy { it.id }
        val dashboard = client.get("/api/v1/clinic/kpis") { bearerAuth(reception) }

        // Assert
        assertEquals(AppointmentStatus.Confirmed, json.decodeFromString<ClinicAppointment>(confirmed.bodyAsText()).status)
        assertEquals(HttpStatusCode.Conflict, confirmedAgain.status)
        assertEquals(ErrorCode.APPOINTMENT_TRANSITION_INVALID, error(confirmedAgain).code)
        assertEquals(HttpStatusCode.NotFound, fromOtherClinic.status)
        assertEquals(AppointmentStatus.Declined, json.decodeFromString<ClinicAppointment>(declined.bodyAsText()).status)
        assertEquals(AppointmentStatus.Confirmed, seenByPatient.getValue(first.id).status)
        assertEquals(AppointmentStatus.Declined, seenByPatient.getValue(second.id).status)
        assertTrue(seenByPatient.getValue(second.id).declineReason.contains("Médico em congresso"))
        assertEquals(HttpStatusCode.OK, dashboard.status)
        val kpis = json.decodeFromString<ClinicDashboard>(dashboard.bodyAsText())
        assertTrue(kpis.received >= 2)
        assertTrue(kpis.confirmation.denominator >= 2)
    }

    @Test
    fun theMedcallPanelNeverReturnsPatientData() = staffTest {
        // Arrange: a patient with recognizable data books an appointment.
        val medcall = signIn(ADMIN_EMAIL, ADMIN_PASSWORD)
        val patient = registerPatient(name = "Zuleica Privada Teste", phone = "(81) 98765-4321")
        book(patient, "Urologia", 0)
        val clinics = json.decodeFromString<List<ManagedClinic>>(client.get("/api/v1/admin/clinics") { bearerAuth(medcall) }.bodyAsText())

        // Act: every read route of the panel.
        val bodies = buildList {
            add(client.get("/api/v1/admin/kpis") { bearerAuth(medcall) }.bodyAsText())
            add(client.get("/api/v1/admin/clinics") { bearerAuth(medcall) }.bodyAsText())
            add(client.get("/api/v1/admin/users") { bearerAuth(medcall) }.bodyAsText())
            add(client.get("/api/v1/admin/professionals") { bearerAuth(medcall) }.bodyAsText())
            add(client.get("/api/v1/admin/audit") { bearerAuth(medcall) }.bodyAsText())
            clinics.forEach { clinic ->
                add(client.get("/api/v1/admin/clinics/${clinic.id}/kpis") { bearerAuth(medcall) }.bodyAsText())
                add(client.get("/api/v1/admin/clinics/${clinic.id}/users") { bearerAuth(medcall) }.bodyAsText())
            }
        }

        // Assert
        bodies.forEach { body ->
            assertFalse(body.contains("Zuleica"), "Patient name leaked: $body")
            assertFalse(body.contains("98765"), "Patient phone leaked")
            assertFalse(body.contains(patient.email), "Patient e-mail leaked")
            assertFalse(body.contains("patientName") || body.contains("patientPhone"), "Appointment rows leaked")
        }
    }

    // --- helpers ---

    private fun staffTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        block()
    }

    private inline fun <reified T> io.ktor.client.request.HttpRequestBuilder.jsonBody(value: T) {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(value))
    }

    private suspend fun error(response: HttpResponse): ApiError = json.decodeFromString(response.bodyAsText())

    private suspend fun ApplicationTestBuilder.signIn(email: String, password: String): String {
        val response = client.post("/api/v1/staff/auth/login") { jsonBody(StaffSignInRequest(email, SecretString(password))) }
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        return json.decodeFromString<StaffAccount>(response.bodyAsText()).sessionToken.value
    }

    private suspend fun ApplicationTestBuilder.newClinic(medcall: String): ManagedClinic {
        val response = client.post("/api/v1/admin/clinics") {
            bearerAuth(medcall)
            jsonBody(SaveClinicRequest("Clínica Teste ${System.nanoTime()}", "Recife", "America/Recife"))
        }
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.createUser(token: String, path: String, role: StaffRole): Pair<StaffUser, SecretString> {
        val response = client.post(path) {
            bearerAuth(token)
            jsonBody(CreateStaffUserRequest("Pessoa ${role.name}", "staff.${System.nanoTime()}@clinica.test", role))
        }
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        val created = json.decodeFromString<StaffUserWithPassword>(response.bodyAsText())
        return created.user to created.temporaryPassword
    }

    /** A clinic account that already changed its temporary password. */
    private suspend fun ApplicationTestBuilder.activeUser(medcall: String, clinic: ManagedClinic, role: StaffRole): String {
        val (user, temporary) = createUser(medcall, "/api/v1/admin/clinics/${clinic.id}/users", role)
        val token = signIn(user.email, temporary.value)
        client.post("/api/v1/staff/auth/password") {
            bearerAuth(token)
            jsonBody(ChangeStaffPasswordRequest(temporary, SecretString("senha-definitiva")))
        }
        return token
    }

    private suspend fun ApplicationTestBuilder.registerPatient(
        name: String = "Paciente do Portal",
        phone: String = "(81) 99999-0000",
    ): AuthenticatedAccount {
        val response = client.post("/api/v1/auth/register") {
            jsonBody(
                RegisterAccountRequest(
                    fullName = name,
                    socialName = "",
                    email = "portal.${System.nanoTime()}@email.com",
                    password = SecretString("segredo"),
                    primaryPhone = Phone.parse(phone),
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

    /** Books the [index]-th open time of the first open day for [specialtyName]. */
    private suspend fun ApplicationTestBuilder.book(patient: AuthenticatedAccount, specialtyName: String, index: Int): Appointment {
        val token = patient.sessionToken.value
        val specialty = json.decodeFromString<List<Specialty>>(client.get("/api/v1/specialties") { bearerAuth(token) }.bodyAsText())
            .first { it.name == specialtyName }
        val day = json.decodeFromString<List<ClinicAvailability>>(
            client.get("/api/v1/availability?specialtyId=${specialty.id}") { bearerAuth(token) }.bodyAsText(),
        ).first().days.first { it.times.size > index }
        val response = client.post("/api/v1/appointments") {
            bearerAuth(token)
            jsonBody(CreateAppointmentRequest(specialty.id, null, date = day.date, time = day.times[index], reason = "Teste", notes = ""))
        }
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return json.decodeFromString(response.bodyAsText())
    }

    private companion object {
        // Set for the test task in server/build.gradle.kts.
        const val ADMIN_EMAIL = "admin@medcall.test"
        const val ADMIN_PASSWORD = "medcall-test-admin"
    }
}
