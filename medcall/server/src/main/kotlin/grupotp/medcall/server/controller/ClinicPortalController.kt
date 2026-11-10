package grupotp.medcall.server.controller

import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.clinic.AttendanceRequest
import grupotp.medcall.common.clinic.DeclineRequest
import grupotp.medcall.common.clinic.RescheduleRequest
import grupotp.medcall.common.clinic.EventOrigin
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.staff.CreateStaffUserRequest
import grupotp.medcall.common.staff.UpdateStaffUserRequest
import grupotp.medcall.server.database.Actor
import grupotp.medcall.server.database.wireValue
import grupotp.medcall.server.plugin.CLINIC_ADMIN
import grupotp.medcall.server.plugin.CLINIC_ROLES
import grupotp.medcall.server.plugin.staffRoute
import grupotp.medcall.server.repository.AgendaFilter
import grupotp.medcall.server.repository.AuditRepository
import grupotp.medcall.server.service.AgendaResult
import grupotp.medcall.server.service.ClinicAgendaService
import grupotp.medcall.server.service.KpiService
import grupotp.medcall.server.service.StaffService
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.ktor.ext.inject

/** Longest period a dashboard or list accepts. */
internal const val MAX_PERIOD_DAYS = 366

/**
 * `from` and `to` (yyyy-MM-dd) of the query, by default the [defaultDays] days ending [today].
 * Null after answering 400 when they are invalid.
 */
internal suspend fun ApplicationCall.period(today: LocalDate, defaultDays: Int = 30): ClosedRange<LocalDate>? {
    // `days` picks the length of a period ending today, in the clinic's own calendar.
    val requestedDays = request.queryParameters["days"]?.toIntOrNull()?.takeIf { it in 1..MAX_PERIOD_DAYS }
    @Suppress("NAME_SHADOWING")
    val defaultDays = requestedDays ?: defaultDays
    val to =request.queryParameters["to"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return badPeriod() } ?: today
    val from = request.queryParameters["from"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return badPeriod() }
        ?: to.minus(DatePeriod(days = defaultDays - 1))
    if (from > to || from.daysUntil(to) >= MAX_PERIOD_DAYS) {
        return badPeriod()
    }
    return from..to
}

private suspend fun ApplicationCall.badPeriod(): ClosedRange<LocalDate>? {
    respondError(HttpStatusCode.BadRequest, grupotp.medcall.server.domain.ServiceError(ErrorCode.PERIOD_INVALID, MAX_PERIOD_DAYS))
    return null
}

internal fun ApplicationCall.intQuery(name: String): Int? = request.queryParameters[name]?.toIntOrNull()

internal fun ApplicationCall.idParameter(): Int? = parameters["id"]?.toIntOrNull()

/** Answers an agenda action. */
internal suspend fun ApplicationCall.respondAgenda(result: AgendaResult) {
    when (result) {
        is AgendaResult.Done -> respond(result.appointment)
        AgendaResult.NotFound -> respondError(HttpStatusCode.NotFound, ErrorCode.APPOINTMENT_NOT_FOUND)
        is AgendaResult.Rejected -> respondError(HttpStatusCode.Conflict, result.error)
        AgendaResult.Changed -> respondError(HttpStatusCode.Conflict, ErrorCode.APPOINTMENT_CHANGED)
        AgendaResult.SlotTaken -> respondError(HttpStatusCode.Conflict, ErrorCode.SLOT_TAKEN)
    }
}

/**
 * Portal da Clínica. The clinic always comes from the signed-in account; a request can never
 * name another clinic.
 */
fun Application.configureClinicPortalController() {
    val agenda by inject<ClinicAgendaService>()
    val kpis by inject<KpiService>()
    val staffService by inject<StaffService>()
    val audit by inject<AuditRepository>()

    routing {
        staffRoute("/api/v1/clinic", CLINIC_ROLES) {
            get("/catalog") {
                val catalog = agenda.catalog(call.staffClinicId)
                if (catalog == null) call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND) else call.respond(catalog)
            }
            agendaRoutes(agenda)
        }
        staffRoute("/api/v1/clinic", CLINIC_ADMIN) {
            get("/kpis") {
                val clinicId = call.staffClinicId
                val period = call.period(agenda.today(clinicId) ?: return@get) ?: return@get
                val dashboard = kpis.clinicDashboard(
                    clinicId,
                    period.start,
                    period.endInclusive,
                    specialtyId = call.intQuery("specialtyId"),
                    professionalId = call.intQuery("professionalId"),
                )
                if (dashboard == null) call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND) else call.respond(dashboard)
            }
            get("/export/appointments.csv") {
                val clinicId = call.staffClinicId
                val period = call.period(agenda.today(clinicId) ?: return@get, defaultDays = 90) ?: return@get
                val csv = kpis.clinicCsv(clinicId, period.start, period.endInclusive) ?: return@get
                val name = "medcall-agendamentos-${period.start}-a-${period.endInclusive}.csv"
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, name).toString(),
                )
                call.respondText(csv, ContentType.Text.CSV.withCharset(Charsets.UTF_8))
            }
            get("/users") {
                call.respond(staffService.clinicTeam(call.staffClinicId))
            }
            post("/users") {
                val result = staffService.createClinicUser(call.staff, call.staffClinicId, call.receive<CreateStaffUserRequest>())
                call.respondResult(result, ErrorCode.USER_NOT_FOUND, created = true)
            }
            put("/users/{id}") {
                val id = call.idParameter() ?: return@put call.respondError(HttpStatusCode.NotFound, ErrorCode.USER_NOT_FOUND)
                val result = staffService.updateClinicUser(call.staff, call.staffClinicId, id, call.receive<UpdateStaffUserRequest>())
                call.respondResult(result, ErrorCode.USER_NOT_FOUND)
            }
            post("/users/{id}/reset-password") {
                val id = call.idParameter() ?: return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.USER_NOT_FOUND)
                call.respondResult(staffService.resetPassword(call.staff, call.staffClinicId, id), ErrorCode.USER_NOT_FOUND)
            }
            get("/audit") {
                call.respond(audit.list(call.staffClinicId, limit = 100))
            }
        }
    }
}

private fun Route.agendaRoutes(agenda: ClinicAgendaService) {
    fun ApplicationCall.actor() = Actor(EventOrigin.ClinicPortal, staffUserId = staff.staffId)

    get("/appointments") {
        val clinicId = call.staffClinicId
        val today = agenda.today(clinicId) ?: return@get call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
        val from = call.request.queryParameters["from"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
        val to = call.request.queryParameters["to"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: from
        if (from > to || from.daysUntil(to) >= MAX_PERIOD_DAYS) {
            return@get call.respondError(HttpStatusCode.BadRequest, grupotp.medcall.server.domain.ServiceError(ErrorCode.PERIOD_INVALID, MAX_PERIOD_DAYS))
        }
        val statuses = call.request.queryParameters["status"]
            ?.split(",")
            ?.mapNotNull { name -> runCatching { wireValue<AppointmentStatus>(name.trim()) }.getOrNull() }
            ?.toSet()
            .orEmpty()
        val filter = AgendaFilter(
            from = from,
            to = to,
            statuses = statuses,
            professionalId = call.intQuery("professionalId"),
            specialtyId = call.intQuery("specialtyId"),
            attendanceMissing = call.request.queryParameters["attendance"] == "missing",
        )
        call.respond(agenda.list(clinicId, filter))
    }
    get("/appointments/pending-count") {
        call.respond(buildJsonObject { put("pending", agenda.pendingCount(call.staffClinicId)) })
    }
    get("/appointments/{id}") {
        val detail = call.idParameter()?.let { agenda.detail(call.staffClinicId, it) }
        if (detail == null) call.respondError(HttpStatusCode.NotFound, ErrorCode.APPOINTMENT_NOT_FOUND) else call.respond(detail)
    }
    post("/appointments/{id}/confirm") {
        val id = call.idParameter() ?: return@post call.respondAgenda(AgendaResult.NotFound)
        call.respondAgenda(agenda.confirm(call.staffClinicId, id, call.actor()))
    }
    post("/appointments/{id}/decline") {
        val id = call.idParameter() ?: return@post call.respondAgenda(AgendaResult.NotFound)
        call.respondAgenda(agenda.decline(call.staffClinicId, id, call.receive<DeclineRequest>(), call.actor()))
    }
    post("/appointments/{id}/reschedule") {
        val id = call.idParameter() ?: return@post call.respondAgenda(AgendaResult.NotFound)
        call.respondAgenda(agenda.reschedule(call.staffClinicId, id, call.receive<RescheduleRequest>(), call.actor()))
    }
    post("/appointments/{id}/attendance") {
        val id = call.idParameter() ?: return@post call.respondAgenda(AgendaResult.NotFound)
        call.respondAgenda(agenda.markAttendance(call.staffClinicId, id, call.receive<AttendanceRequest>().attended, call.actor()))
    }
    post("/appointments/{id}/attendance/undo") {
        val id = call.idParameter() ?: return@post call.respondAgenda(AgendaResult.NotFound)
        call.respondAgenda(agenda.undoAttendance(call.staffClinicId, id, call.actor()))
    }
    get("/professionals/{id}/free-days") {
        val days = call.idParameter()?.let { agenda.freeDays(call.staffClinicId, it) }
        if (days == null) call.respondError(HttpStatusCode.NotFound, ErrorCode.PROFESSIONAL_NOT_FOUND) else call.respond(days)
    }
}
