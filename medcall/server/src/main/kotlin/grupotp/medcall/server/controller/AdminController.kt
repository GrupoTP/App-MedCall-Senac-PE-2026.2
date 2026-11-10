package grupotp.medcall.server.controller

import grupotp.medcall.common.admin.ChangeClinicStatusRequest
import grupotp.medcall.common.admin.SaveClinicRequest
import grupotp.medcall.common.admin.SaveProfessionalRequest
import grupotp.medcall.common.admin.SaveSpecialtyRequest
import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.staff.CreateStaffUserRequest
import grupotp.medcall.server.plugin.MEDCALL_ADMIN
import grupotp.medcall.server.plugin.staffRoute
import grupotp.medcall.server.repository.AuditRepository
import grupotp.medcall.server.service.AdminService
import grupotp.medcall.server.service.KpiService
import grupotp.medcall.server.service.StaffService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.time.Clock
import kotlin.time.toKotlinInstant

@Serializable
private data class ActiveRequest(val active: Boolean)

/**
 * Painel de Controle MedCall. Only aggregates and registrations: no route here returns an
 * appointment row or patient data (LGPD, see docs/kpis.md and SPM-96).
 */
fun Application.configureAdminController() {
    val admin by inject<AdminService>()
    val kpis by inject<KpiService>()
    val staffService by inject<StaffService>()
    val audit by inject<AuditRepository>()
    val clock by inject<Clock>()

    routing {
        staffRoute("/api/v1/admin", MEDCALL_ADMIN) {
            get("/kpis") {
                val today = clock.instant().toKotlinInstant().toLocalDateTime(TimeZone.UTC).date
                val period = call.period(today) ?: return@get
                val clinicIds = call.request.queryParameters["clinicId"]
                    ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()?.ifEmpty { null }
                call.respond(kpis.platformDashboard(period.start, period.endInclusive, clinicIds))
            }
            get("/time-zones") {
                call.respond(AdminService.BRAZIL_TIME_ZONES)
            }
            get("/clinics") {
                call.respond(admin.clinics())
            }
            post("/clinics") {
                call.respondResult(admin.createClinic(call.staff, call.receive<SaveClinicRequest>()), ErrorCode.CLINIC_NOT_FOUND, created = true)
            }
            put("/clinics/{id}") {
                val id = call.idParameter() ?: return@put call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                call.respondResult(admin.updateClinic(call.staff, id, call.receive<SaveClinicRequest>()), ErrorCode.CLINIC_NOT_FOUND)
            }
            post("/clinics/{id}/status") {
                val id = call.idParameter() ?: return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                val status = call.receive<ChangeClinicStatusRequest>().status
                call.respondResult(admin.setClinicStatus(call.staff, id, status), ErrorCode.CLINIC_NOT_FOUND)
            }
            get("/clinics/{id}/kpis") {
                val id = call.idParameter() ?: return@get call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                val clinic = admin.clinic(id) ?: return@get call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                val today = clock.instant().toKotlinInstant().toLocalDateTime(TimeZone.of(clinic.timeZoneId)).date
                val period = call.period(today) ?: return@get
                val dashboard = kpis.clinicDashboardForMedcall(id, period.start, period.endInclusive)
                    ?: return@get call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                call.respond(dashboard)
            }
            get("/clinics/{id}/users") {
                val id = call.idParameter() ?: return@get call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                if (admin.clinic(id) == null) return@get call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                call.respond(staffService.clinicTeam(id))
            }
            post("/clinics/{id}/users") {
                val id = call.idParameter() ?: return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                if (admin.clinic(id) == null) return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.CLINIC_NOT_FOUND)
                call.respondResult(staffService.createClinicUser(call.staff, id, call.receive<CreateStaffUserRequest>()), ErrorCode.USER_NOT_FOUND, created = true)
            }
            get("/users") {
                call.respond(staffService.allClinicStaff())
            }
            post("/users/{id}/reset-password") {
                val id = call.idParameter() ?: return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.USER_NOT_FOUND)
                call.respondResult(staffService.resetPassword(call.staff, null, id), ErrorCode.USER_NOT_FOUND)
            }
            post("/users/{id}/active") {
                val id = call.idParameter() ?: return@post call.respondError(HttpStatusCode.NotFound, ErrorCode.USER_NOT_FOUND)
                call.respondResult(staffService.setActiveByMedcall(call.staff, id, call.receive<ActiveRequest>().active), ErrorCode.USER_NOT_FOUND)
            }
            get("/specialties") {
                call.respond(admin.specialties())
            }
            post("/specialties") {
                call.respondResult(admin.createSpecialty(call.staff, call.receive<SaveSpecialtyRequest>().name), ErrorCode.SPECIALTY_NOT_FOUND, created = true)
            }
            get("/professionals") {
                call.respond(admin.professionals(call.intQuery("clinicId")))
            }
            post("/professionals") {
                call.respondResult(admin.createProfessional(call.staff, call.receive<SaveProfessionalRequest>()), ErrorCode.PROFESSIONAL_NOT_FOUND, created = true)
            }
            put("/professionals/{id}") {
                val id = call.idParameter() ?: return@put call.respondError(HttpStatusCode.NotFound, ErrorCode.PROFESSIONAL_NOT_FOUND)
                call.respondResult(admin.updateProfessional(call.staff, id, call.receive<SaveProfessionalRequest>()), ErrorCode.PROFESSIONAL_NOT_FOUND)
            }
            get("/audit") {
                call.respond(audit.list(clinicId = call.intQuery("clinicId"), limit = 200))
            }
        }
    }
}
