package grupotp.medcall.server.plugin

import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.server.controller.respondError
import grupotp.medcall.server.security.StaffPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.principal
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.route

/**
 * Route at [path] that only answers a signed-in person: anyone else gets 401 from the
 * [configurations] providers, the session JWT ([SESSION_AUTH]) by default.
 */
fun Route.authenticatedRoute(path: String, vararg configurations: String? = arrayOf(SESSION_AUTH), build: Route.() -> Unit) {
    authenticate(configurations = configurations) {
        route(path, build)
    }
}

/** Roles a [staffRoute] answers. */
class StaffRolesConfig {
    var roles: Set<StaffRole> = emptySet()
    var allowTemporaryPassword: Boolean = false
}

/**
 * Refuses, after authentication, a staff member whose role is not allowed (403 FORBIDDEN), or who
 * still has a temporary password (403 PASSWORD_CHANGE_REQUIRED).
 */
private val StaffRoles = createRouteScopedPlugin("StaffRoles", ::StaffRolesConfig) {
    val roles = pluginConfig.roles
    val allowTemporaryPassword = pluginConfig.allowTemporaryPassword
    on(AuthenticationChecked) { call ->
        val principal = call.principal<StaffPrincipal>() ?: return@on
        when {
            principal.role !in roles -> call.respondError(HttpStatusCode.Forbidden, ErrorCode.FORBIDDEN)
            principal.mustChangePassword && !allowTemporaryPassword ->
                call.respondError(HttpStatusCode.Forbidden, ErrorCode.PASSWORD_CHANGE_REQUIRED)
        }
    }
}

/**
 * Route at [path] for signed-in staff with one of [roles]. Anyone else gets 401 (no staff session)
 * or 403 (another role). Clinic routes read the clinic from the principal, never from the request,
 * so one clinic can never name another's data.
 */
fun Route.staffRoute(
    path: String,
    roles: Set<StaffRole>,
    allowTemporaryPassword: Boolean = false,
    build: Route.() -> Unit,
) {
    authenticate(STAFF_AUTH) {
        route(path) {
            // A child per set of roles: the same path can hold routes for different roles, and
            // Ktor merges equal routes, which would install the check twice on one of them.
            val limited = createChild(RolesSelector(roles, allowTemporaryPassword))
            limited.install(StaffRoles) {
                this.roles = roles
                this.allowTemporaryPassword = allowTemporaryPassword
            }
            limited.build()
        }
    }
}

/** Matches everything; only tells routes limited to different roles apart. */
private data class RolesSelector(val roles: Set<StaffRole>, val allowTemporaryPassword: Boolean) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Transparent
}

val CLINIC_ROLES: Set<StaffRole> = setOf(StaffRole.ClinicAdmin, StaffRole.Receptionist)
val CLINIC_ADMIN: Set<StaffRole> = setOf(StaffRole.ClinicAdmin)
val MEDCALL_ADMIN: Set<StaffRole> = setOf(StaffRole.MedcallAdmin)
val ALL_STAFF: Set<StaffRole> = StaffRole.entries.toSet()
