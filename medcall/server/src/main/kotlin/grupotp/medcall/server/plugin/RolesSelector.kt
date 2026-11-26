package grupotp.medcall.server.plugin

import grupotp.medcall.common.staff.StaffRole
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

/** Matches everything; only tells routes limited to different roles apart. */
internal data class RolesSelector(val roles: Set<StaffRole>, val allowTemporaryPassword: Boolean) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Transparent
}

