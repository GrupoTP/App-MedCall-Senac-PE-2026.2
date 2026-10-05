package grupotp.recifesaudavel.server.controller

import cc.rbbl.ktor_health_check.Health
import grupotp.recifesaudavel.server.service.HealthService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.ext.inject

/**
 * Exposes liveness and readiness using the health-check plugin.
 * The plugin owns the HTTP routes; this controller only delegates to [HealthService].
 */
fun Application.configureHealthController() {
    val healthService by inject<HealthService>()
    install(Health) {
        healthCheck("application") { healthService.isApplicationUp() }
        readyCheck("application") { healthService.isApplicationUp() }
    }
}
