package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.HealthStatus
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

/**
 * Decides whether this process is alive and whether it can serve traffic.
 */
class HealthService(
    private val database: Database,
) {
    fun healthStatus(): HealthStatus = HealthStatus(
        service = "Recife Saudável",
        status = "ok",
    )

    fun isApplicationUp(): Boolean = healthStatus().status == "ok"

    /**
     * True when the database answers within [DATABASE_TIMEOUT_SECONDS]. Uses the JDBC
     * `isValid` check, so it works the same on H2 and MySQL.
     */
    fun isDatabaseReachable(): Boolean = try {
        transaction(database) {
            (connection.connection as Connection).isValid(DATABASE_TIMEOUT_SECONDS)
        }
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val DATABASE_TIMEOUT_SECONDS = 2
    }
}
