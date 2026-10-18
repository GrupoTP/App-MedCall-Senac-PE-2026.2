package grupotp.recifesaudavel.server.plugin

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
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
