package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.HealthStatus

/**
 * Decides whether this process can serve traffic.
 */
class HealthService {
    fun healthStatus(): HealthStatus = HealthStatus(
        service = "Recife Saudável",
        status = "ok",
    )

    fun isApplicationUp(): Boolean = healthStatus().status == "ok"
}
