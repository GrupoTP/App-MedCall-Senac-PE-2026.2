package grupotp.medcall.server.controller

import grupotp.medcall.common.api.ApiError
import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.clinic.AttendanceRequest
import grupotp.medcall.common.clinic.DeclineRequest
import grupotp.medcall.common.clinic.EventOrigin
import grupotp.medcall.common.clinic.RescheduleRequest
import grupotp.medcall.common.integration.CreateApiKeyRequest
import grupotp.medcall.common.integration.SaveWebhookRequest
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.server.database.Actor
import grupotp.medcall.server.database.wireName
import grupotp.medcall.server.database.wireValue
import grupotp.medcall.server.domain.ServiceError
import grupotp.medcall.server.i18n.ErrorMessages
import grupotp.medcall.server.integration.ApiPrincipal
import grupotp.medcall.server.integration.IntegrationRepository
import grupotp.medcall.server.integration.IntegrationService
import grupotp.medcall.server.plugin.API_KEY_AUTH
import grupotp.medcall.server.plugin.CLINIC_ADMIN
import grupotp.medcall.server.plugin.staffRoute
import grupotp.medcall.server.service.AgendaResult
import grupotp.medcall.server.service.ClinicAgendaService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import kotlin.time.toKotlinInstant

/** Webhooks, deliveries and API keys in the clinic portal (SPM-97, SPM-98, SPM-100), and the public API (SPM-99). */
fun Application.configureIntegrationController() {
    val integration by inject<IntegrationService>()
    val repository by inject<IntegrationRepository>()
    val agenda by inject<ClinicAgendaService>()
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    routing {
        clinicIntegrationRoutes(integration)
        publicApiRoutes(integration, repository, agenda, json)
        get("/api/public/docs") {
            call.respondText(DOCS_HTML, ContentType.Text.Html)
        }
        get("/api/public/openapi.yaml") {
            val spec = Application::class.java.classLoader.getResource("public-api/openapi.yaml")!!.readText()
            call.respondText(spec, ContentType.parse("application/yaml"))
        }
    }
}

private fun Route.clinicIntegrationRoutes(integration: IntegrationService) {
    staffRoute("/api/v1/clinic/webhooks", CLINIC_ADMIN) {
        get { call.respond(integration.webhooks(call.staffClinicId)) }
        post {
            val request = call.receiveJson<SaveWebhookRequest>()
            call.respondResult(integration.createWebhook(call.staff, call.staffClinicId, request), ErrorCode.WEBHOOK_NOT_FOUND, created = true)
        }
        put("/{id}") {
            val id = call.idParameter() ?: return@put call.respondError(HttpStatusCode.NotFound, ErrorCode.WEBHOOK_NOT_FOUND)
            call.respondResult(integration.updateWebhook(call.staff, call.staffClinicId, id, call.receiveJson()), ErrorCode.WEBHOOK_NOT_FOUND)
        }
        post("/{id}/rotate-secret") {
            val id = call.idParameter() ?: return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.WEBHOOK_NOT_FOUND)
            call.respondResult(integration.rotateSecret(call.staff, call.staffClinicId, id), ErrorCode.WEBHOOK_NOT_FOUND)
        }
        post("/{id}/test") {
            val id = call.idParameter() ?: return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.WEBHOOK_NOT_FOUND)
            call.respondResult(integration.test(call.staffClinicId, id), ErrorCode.WEBHOOK_NOT_FOUND)
        }
    }
    staffRoute("/api/v1/clinic/webhook-deliveries", CLINIC_ADMIN) {
        get { call.respond(integration.deliveries(call.staffClinicId, call.intQuery("webhookId"))) }
        post("/{id}/redeliver") {
            val id = call.idParameter() ?: return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.WEBHOOK_NOT_FOUND)
            call.respondResult(integration.redeliver(call.staffClinicId, id), ErrorCode.WEBHOOK_NOT_FOUND)
        }
    }
    staffRoute("/api/v1/clinic/api-keys", CLINIC_ADMIN) {
        get { call.respond(integration.apiKeys(call.staffClinicId)) }
        post {
            val request = call.receiveJson<CreateApiKeyRequest>()
            call.respondResult(integration.createApiKey(call.staff, call.staffClinicId, request.name), ErrorCode.API_KEY_NOT_FOUND, created = true)
        }
        post("/{id}/revoke") {
            val id = call.idParameter() ?: return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.API_KEY_NOT_FOUND)
            call.respondResult(integration.revokeApiKey(call.staff, call.staffClinicId, id), ErrorCode.API_KEY_NOT_FOUND)
        }
    }
}

private suspend inline fun <reified T> ApplicationCall.receiveJson(): T =
    Json { ignoreUnknownKeys = true }.decodeFromString(receiveText())

/**
 * The public API: the clinic's own system answers appointments with an API key. Same rules as the
 * portal (ClinicAgendaService); `Idempotency-Key` makes retries safe; 60 calls per minute per key.
 */
private fun Route.publicApiRoutes(
    integration: IntegrationService,
    repository: IntegrationRepository,
    agenda: ClinicAgendaService,
    json: Json,
) {
    authenticate(API_KEY_AUTH) {
        route("/api/public/v1") {
            get("/appointments") {
                val api = call.apiPrincipal() ?: return@get
                val today = agenda.today(api.clinicId) ?: return@get call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                val from = call.request.queryParameters["from"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
                val to = call.request.queryParameters["to"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: from.plus(DatePeriod(days = 13))
                if (from > to || from.daysUntil(to) >= MAX_PERIOD_DAYS) {
                    return@get call.respondError(HttpStatusCode.BadRequest, ServiceError(ErrorCode.PERIOD_INVALID, MAX_PERIOD_DAYS))
                }
                val status = call.request.queryParameters["status"]?.let { name ->
                    runCatching { wireValue<AppointmentStatus>(name).wireName() }.getOrNull()
                        ?: return@get call.respondError(HttpStatusCode.BadRequest, ErrorCode.INVALID_CHOICE)
                }
                call.respond(repository.publicAppointments(api.clinicId, from = from, to = to, status = status))
            }
            post("/appointments/{id}/{action}") {
                val api = call.apiPrincipal() ?: return@post
                val body = call.receiveText()
                val id = call.idParameter()
                val action = call.parameters["action"]
                idempotent(api, body, repository) {
                    if (id == null) return@idempotent errorBody(HttpStatusCode.NotFound, ServiceError(ErrorCode.APPOINTMENT_NOT_FOUND))
                    val actor = Actor(EventOrigin.PublicApi, apiKeyId = api.apiKeyId)
                    val result = try {
                        when (action) {
                            "confirm" -> agenda.confirm(api.clinicId, id, actor)
                            "decline" -> agenda.decline(api.clinicId, id, json.decodeFromString<DeclineRequest>(body), actor)
                            "reschedule" -> agenda.reschedule(api.clinicId, id, json.decodeFromString<RescheduleRequest>(body), actor)
                            "attendance" -> agenda.markAttendance(api.clinicId, id, json.decodeFromString<AttendanceRequest>(body).attended, actor)
                            else -> return@idempotent errorBody(HttpStatusCode.NotFound, ServiceError(ErrorCode.INVALID_CHOICE))
                        }
                    } catch (_: kotlinx.serialization.SerializationException) {
                        return@idempotent errorBody(HttpStatusCode.BadRequest, ServiceError(ErrorCode.INVALID_CHOICE))
                    }
                    when (result) {
                        is AgendaResult.Done -> HttpStatusCode.OK to json.encodeToString(
                            repository.publicAppointments(api.clinicId, ids = listOf(id)).single(),
                        )
                        AgendaResult.NotFound -> errorBody(HttpStatusCode.NotFound, ServiceError(ErrorCode.APPOINTMENT_NOT_FOUND))
                        is AgendaResult.Rejected -> errorBody(HttpStatusCode.Conflict, result.error)
                        AgendaResult.Changed -> errorBody(HttpStatusCode.Conflict, ServiceError(ErrorCode.APPOINTMENT_CHANGED))
                        AgendaResult.SlotTaken -> errorBody(HttpStatusCode.Conflict, ServiceError(ErrorCode.SLOT_TAKEN))
                    }
                }
            }
        }
    }
}

/** The key's principal, after the rate limit; null after answering 429. */
private suspend fun ApplicationCall.apiPrincipal(): ApiPrincipal? {
    val principal = checkNotNull(principal<ApiPrincipal>())
    val integration = application.inject<IntegrationService>().value
    if (!integration.withinRateLimit(principal.apiKeyId)) {
        response.headers.append("Retry-After", "60")
        respondError(HttpStatusCode.TooManyRequests, ErrorCode.RATE_LIMITED)
        return null
    }
    return principal
}

private fun errorBody(status: HttpStatusCode, error: ServiceError): Pair<HttpStatusCode, String> =
    status to Json.encodeToString(ApiError(error.code, ErrorMessages.of(error.code, error.arguments)))

/**
 * Runs [action] once per `Idempotency-Key`: a repeat with the same body gets the stored answer,
 * a repeat with another body gets 422. Without the header, the action just runs.
 */
private suspend fun io.ktor.server.routing.RoutingContext.idempotent(
    api: ApiPrincipal,
    body: String,
    repository: IntegrationRepository,
    action: suspend () -> Pair<HttpStatusCode, String>,
) {
    val key = call.request.headers["Idempotency-Key"]?.trim()?.takeIf { it.isNotEmpty() }?.take(100)
    val hash = IntegrationService.sha256(call.request.httpMethod.value + " " + call.request.path() + "\n" + body)
    if (key != null) {
        val stored = repository.idempotency(api.apiKeyId, key)
        if (stored != null) {
            if (stored.requestHash != hash) {
                call.respondError(HttpStatusCode.UnprocessableEntity, ErrorCode.IDEMPOTENCY_KEY_REUSED)
            } else {
                call.response.headers.append("Idempotent-Replayed", "true")
                call.respondText(stored.body, ContentType.Application.Json, HttpStatusCode.fromValue(stored.statusCode))
            }
            return
        }
    }
    val (status, json) = action()
    if (key != null) {
        repository.storeIdempotency(api.apiKeyId, key, hash, status.value, json, java.time.Clock.systemUTC().instant().toKotlinInstant())
    }
    call.respondText(json, ContentType.Application.Json, status)
}

private val DOCS_HTML = """
<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>API pública MedCall</title>
  <style>body { margin: 0; font-family: system-ui, sans-serif; }</style>
</head>
<body>
  <redoc spec-url="openapi.yaml" hide-download-button="false"></redoc>
  <script src="https://cdn.jsdelivr.net/npm/redoc@2.5.4/bundles/redoc.standalone.js"></script>
</body>
</html>
""".trimIndent()
