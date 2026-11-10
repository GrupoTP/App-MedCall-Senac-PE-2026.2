package grupotp.medcall.admin.api

import grupotp.medcall.common.admin.AuditEntry
import grupotp.medcall.common.admin.ChangeClinicStatusRequest
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.admin.ManagedProfessional
import grupotp.medcall.common.admin.SaveClinicRequest
import grupotp.medcall.common.admin.SaveProfessionalRequest
import grupotp.medcall.common.admin.SaveSpecialtyRequest
import grupotp.medcall.common.api.ApiError
import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.clinic.AttendanceRequest
import grupotp.medcall.common.clinic.ClinicAppointment
import grupotp.medcall.common.clinic.ClinicAppointmentDetail
import grupotp.medcall.common.clinic.ClinicCatalog
import grupotp.medcall.common.clinic.DeclineRequest
import grupotp.medcall.common.clinic.ProfessionalFreeDay
import grupotp.medcall.common.clinic.RescheduleRequest
import grupotp.medcall.common.integration.ApiKey
import grupotp.medcall.common.integration.ApiKeyWithSecret
import grupotp.medcall.common.integration.CreateApiKeyRequest
import grupotp.medcall.common.integration.SaveWebhookRequest
import grupotp.medcall.common.integration.Webhook
import grupotp.medcall.common.integration.WebhookDelivery
import grupotp.medcall.common.integration.WebhookTestResult
import grupotp.medcall.common.integration.WebhookWithSecret
import grupotp.medcall.common.kpi.ClinicDashboard
import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.kpi.PlatformDashboard
import grupotp.medcall.common.scheduling.Specialty
import grupotp.medcall.common.security.SecretString
import grupotp.medcall.common.staff.ChangeStaffPasswordRequest
import grupotp.medcall.common.staff.CreateStaffUserRequest
import grupotp.medcall.common.staff.StaffAccount
import grupotp.medcall.common.staff.StaffSignInRequest
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.common.staff.StaffUserWithPassword
import grupotp.medcall.common.staff.UpdateStaffUserRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import web.location.location

/** A refusal of the server, with the text it sent for the person. */
class ApiException(val status: Int, val code: ErrorCode?, override val message: String) : Exception(message)

@Serializable
private data class ActiveRequest(val active: Boolean)

/** A dashboard period: the last [days] days in the clinic's calendar, or [from]..[to]. */
data class PeriodChoice(val days: Int? = 30, val from: LocalDate? = null, val to: LocalDate? = null) {
    fun query(): Array<Pair<String, Any?>> =
        if (from != null && to != null) arrayOf("from" to from, "to" to to) else arrayOf("days" to (days ?: 30))
}

/**
 * The portals' calls to the MedCall server. Every method throws [ApiException] on a refusal; a
 * 401 also calls [onSessionLost], so the screen goes back to sign-in.
 */
class AdminApi(
    private val tokenProvider: () -> String?,
    private val onSessionLost: () -> Unit,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    // --- staff session ---

    suspend fun signIn(email: String, password: String): StaffAccount =
        send<StaffAccount>(authenticated = false) {
            client.post(url("/api/v1/staff/auth/login")) { body(StaffSignInRequest(email, SecretString(password))) }
        }

    suspend fun me(): StaffAccount = get("/api/v1/staff/auth/me")

    suspend fun changePassword(current: String, new: String): StaffAccount =
        post("/api/v1/staff/auth/password", ChangeStaffPasswordRequest(SecretString(current), SecretString(new)))

    suspend fun signOut() {
        runCatching { send<JsonObject> { client.post(url("/api/v1/staff/auth/logout")) { auth() } } }
    }

    // --- clinic portal ---

    suspend fun clinicCatalog(): ClinicCatalog = get("/api/v1/clinic/catalog")

    suspend fun clinicDashboard(period: PeriodChoice, specialtyId: Int?, professionalId: Int?): ClinicDashboard =
        get("/api/v1/clinic/kpis", *period.query(), "specialtyId" to specialtyId, "professionalId" to professionalId)

    suspend fun agenda(
        from: LocalDate,
        to: LocalDate,
        statuses: List<String>,
        professionalId: Int?,
        specialtyId: Int?,
        attendanceMissing: Boolean,
    ): List<ClinicAppointment> = get(
        "/api/v1/clinic/appointments",
        "from" to from,
        "to" to to,
        "status" to statuses.joinToString(",").ifEmpty { null },
        "professionalId" to professionalId,
        "specialtyId" to specialtyId,
        "attendance" to if (attendanceMissing) "missing" else null,
    )

    suspend fun pendingCount(): Int =
        get<JsonObject>("/api/v1/clinic/appointments/pending-count")["pending"]!!.jsonPrimitive.int

    suspend fun appointment(id: Int): ClinicAppointmentDetail = get("/api/v1/clinic/appointments/$id")

    suspend fun confirm(id: Int): ClinicAppointment = postNoBody("/api/v1/clinic/appointments/$id/confirm")

    suspend fun decline(id: Int, request: DeclineRequest): ClinicAppointment = post("/api/v1/clinic/appointments/$id/decline", request)

    suspend fun reschedule(id: Int, request: RescheduleRequest): ClinicAppointment =
        post("/api/v1/clinic/appointments/$id/reschedule", request)

    suspend fun attendance(id: Int, attended: Boolean): ClinicAppointment =
        post("/api/v1/clinic/appointments/$id/attendance", AttendanceRequest(attended))

    suspend fun undoAttendance(id: Int): ClinicAppointment = postNoBody("/api/v1/clinic/appointments/$id/attendance/undo")

    suspend fun freeDays(professionalId: Int): List<ProfessionalFreeDay> = get("/api/v1/clinic/professionals/$professionalId/free-days")

    suspend fun clinicUsers(): List<StaffUser> = get("/api/v1/clinic/users")

    suspend fun createClinicUser(request: CreateStaffUserRequest): StaffUserWithPassword = post("/api/v1/clinic/users", request)

    suspend fun updateClinicUser(id: Int, request: UpdateStaffUserRequest): StaffUser = put("/api/v1/clinic/users/$id", request)

    suspend fun resetClinicUserPassword(id: Int): StaffUserWithPassword = postNoBody("/api/v1/clinic/users/$id/reset-password")

    suspend fun clinicAudit(): List<AuditEntry> = get("/api/v1/clinic/audit")

    /** Downloads the period's CSV with the session token and hands it to the browser as a file. */
    suspend fun downloadCsv(period: PeriodChoice): String {
        val response = client.get(url("/api/v1/clinic/export/appointments.csv")) {
            auth()
            period.query().forEach { (name, value) -> if (value != null) parameter(name, value.toString()) }
        }
        check(response)
        return response.bodyAsText()
    }

    // --- integrations ---

    suspend fun webhooks(): List<Webhook> = get("/api/v1/clinic/webhooks")

    suspend fun createWebhook(request: SaveWebhookRequest): WebhookWithSecret = post("/api/v1/clinic/webhooks", request)

    suspend fun updateWebhook(id: Int, request: SaveWebhookRequest): Webhook = put("/api/v1/clinic/webhooks/$id", request)

    suspend fun rotateWebhookSecret(id: Int): WebhookWithSecret = postNoBody("/api/v1/clinic/webhooks/$id/rotate-secret")

    suspend fun testWebhook(id: Int): WebhookTestResult = postNoBody("/api/v1/clinic/webhooks/$id/test")

    suspend fun deliveries(webhookId: Int?): List<WebhookDelivery> = get("/api/v1/clinic/webhook-deliveries", "webhookId" to webhookId)

    suspend fun redeliver(id: Int): WebhookDelivery = postNoBody("/api/v1/clinic/webhook-deliveries/$id/redeliver")

    suspend fun apiKeys(): List<ApiKey> = get("/api/v1/clinic/api-keys")

    suspend fun createApiKey(name: String): ApiKeyWithSecret = post("/api/v1/clinic/api-keys", CreateApiKeyRequest(name))

    suspend fun revokeApiKey(id: Int): ApiKey = postNoBody("/api/v1/clinic/api-keys/$id/revoke")

    // --- MedCall panel ---

    suspend fun platformDashboard(period: PeriodChoice, clinicIds: List<Int>): PlatformDashboard =
        get("/api/v1/admin/kpis", *period.query(), "clinicId" to clinicIds.joinToString(",").ifEmpty { null })

    suspend fun clinicDashboardForMedcall(clinicId: Int, period: PeriodChoice): ClinicDashboard =
        get("/api/v1/admin/clinics/$clinicId/kpis", *period.query())

    suspend fun timeZones(): List<String> = get("/api/v1/admin/time-zones")

    suspend fun clinics(): List<ManagedClinic> = get("/api/v1/admin/clinics")

    suspend fun createClinic(request: SaveClinicRequest): ManagedClinic = post("/api/v1/admin/clinics", request)

    suspend fun updateClinic(id: Int, request: SaveClinicRequest): ManagedClinic = put("/api/v1/admin/clinics/$id", request)

    suspend fun setClinicStatus(id: Int, status: ClinicStatus): ManagedClinic =
        post("/api/v1/admin/clinics/$id/status", ChangeClinicStatusRequest(status))

    suspend fun clinicTeam(clinicId: Int): List<StaffUser> = get("/api/v1/admin/clinics/$clinicId/users")

    suspend fun createUserForClinic(clinicId: Int, request: CreateStaffUserRequest): StaffUserWithPassword =
        post("/api/v1/admin/clinics/$clinicId/users", request)

    suspend fun allClinicStaff(): List<StaffUser> = get("/api/v1/admin/users")

    suspend fun resetPasswordByMedcall(id: Int): StaffUserWithPassword = postNoBody("/api/v1/admin/users/$id/reset-password")

    suspend fun setUserActive(id: Int, active: Boolean): StaffUser = post("/api/v1/admin/users/$id/active", ActiveRequest(active))

    suspend fun specialties(): List<Specialty> = get("/api/v1/admin/specialties")

    suspend fun createSpecialty(name: String): Specialty = post("/api/v1/admin/specialties", SaveSpecialtyRequest(name))

    suspend fun professionals(clinicId: Int?): List<ManagedProfessional> = get("/api/v1/admin/professionals", "clinicId" to clinicId)

    suspend fun createProfessional(request: SaveProfessionalRequest): ManagedProfessional = post("/api/v1/admin/professionals", request)

    suspend fun updateProfessional(id: Int, request: SaveProfessionalRequest): ManagedProfessional =
        put("/api/v1/admin/professionals/$id", request)

    suspend fun audit(): List<AuditEntry> = get("/api/v1/admin/audit")

    // --- plumbing ---

    private suspend inline fun <reified T> get(path: String, vararg query: Pair<String, Any?>): T = send {
        client.get(url(path)) {
            auth()
            query.forEach { (name, value) -> if (value != null) parameter(name, value.toString()) }
        }
    }

    private suspend inline fun <reified T, reified B : Any> post(path: String, body: B): T = send {
        client.post(url(path)) {
            auth()
            body(body)
        }
    }

    private suspend inline fun <reified T> postNoBody(path: String): T = send {
        client.post(url(path)) { auth() }
    }

    private suspend inline fun <reified T, reified B : Any> put(path: String, body: B): T = send {
        client.put(url(path)) {
            auth()
            body(body)
        }
    }

    private inline fun <reified B : Any> HttpRequestBuilder.body(value: B) {
        contentType(ContentType.Application.Json)
        setBody(value)
    }

    private fun HttpRequestBuilder.auth() {
        tokenProvider()?.let { bearerAuth(it) }
    }

    private suspend inline fun <reified T> send(authenticated: Boolean = true, request: () -> HttpResponse): T {
        val response = try {
            request()
        } catch (error: Throwable) {
            throw ApiException(0, null, "Não foi possível falar com o servidor. Confira a conexão e tente de novo.")
        }
        check(response, authenticated)
        return response.body()
    }

    private suspend fun check(response: HttpResponse, authenticated: Boolean = true) {
        if (response.status.isSuccess()) return
        val error = runCatching { json.decodeFromString<ApiError>(response.bodyAsText()) }.getOrNull()
        if (response.status.value == 401 && authenticated) {
            onSessionLost()
        }
        throw ApiException(
            status = response.status.value,
            code = error?.code,
            message = error?.message ?: "Erro ${response.status.value} no servidor.",
        )
    }

    private fun url(path: String): String = apiBase() + path

    companion object {
        /**
         * Where the API is. Deployed, the portal is served at /admin/ on the API's own host. On the
         * webpack dev server (port 8082) the API runs apart, on port 8080 of the same machine.
         */
        fun apiBase(): String =
            if (location.port == "8082") "${location.protocol}//${location.hostname}:8080" else ""
    }
}
