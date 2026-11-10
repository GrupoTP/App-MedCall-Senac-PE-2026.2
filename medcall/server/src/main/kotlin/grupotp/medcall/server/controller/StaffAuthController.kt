package grupotp.medcall.server.controller

import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.staff.ChangeStaffPasswordRequest
import grupotp.medcall.common.staff.StaffSignInRequest
import grupotp.medcall.server.plugin.ALL_STAFF
import grupotp.medcall.server.plugin.staffRoute
import grupotp.medcall.server.security.StaffPrincipal
import grupotp.medcall.server.service.StaffResult
import grupotp.medcall.server.service.StaffService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import org.koin.ktor.ext.inject

/** The signed-in staff member. Only for routes under `staffRoute`. */
internal val ApplicationCall.staff: StaffPrincipal
    get() = checkNotNull(principal<StaffPrincipal>()) { "Route is not under staffRoute." }

/** The clinic of a clinic-role staff member. Only for routes limited to clinic roles. */
internal val ApplicationCall.staffClinicId: Int
    get() = checkNotNull(staff.clinicId) { "Route is not limited to clinic roles." }

/** Answers a [StaffResult]: 200 (or [created]), 400 with the error, or 404 with [notFound]. */
internal suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: StaffResult<T>,
    notFound: ErrorCode,
    created: Boolean = false,
) {
    when (result) {
        is StaffResult.Done -> respond(if (created) HttpStatusCode.Created else HttpStatusCode.OK, result.value)
        is StaffResult.Rejected -> respondError(HttpStatusCode.BadRequest, result.error)
        StaffResult.NotFound -> respondError(HttpStatusCode.NotFound, notFound)
    }
}

/** Sign-in of the clinic portal and the MedCall panel. */
fun Application.configureStaffAuthController() {
    val staffService by inject<StaffService>()
    routing {
        post("/api/v1/staff/auth/login") {
            val account = staffService.signIn(call.receive<StaffSignInRequest>())
            if (account == null) {
                call.respondError(HttpStatusCode.Unauthorized, ErrorCode.WRONG_CREDENTIALS)
            } else {
                call.respond(account)
            }
        }
        staffRoute("/api/v1/staff/auth", ALL_STAFF, allowTemporaryPassword = true) {
            get("/me") {
                val account = staffService.account(call.staff.staffId)
                if (account == null) call.respondSessionExpired() else call.respond(account)
            }
            post("/password") {
                val result = staffService.changePassword(call.staff, call.receive<ChangeStaffPasswordRequest>())
                call.respondResult(result, ErrorCode.USER_NOT_FOUND)
            }
            post("/logout") {
                staffService.signOut(call.staff.sessionId)
                call.respond(HttpStatusCode.OK, buildJsonObject { })
            }
        }
    }
}
