package grupotp.recifesaudavel.common

import kotlinx.serialization.Serializable

/**
 * Shared health payload returned by the server and shown by the web client.
 */
@Serializable
data class HealthStatus(
    val service: String,
    val status: String,
)

/**
 * Current process status shared by the server and the web client.
 */
fun healthStatus(): HealthStatus = HealthStatus(
    service = "Recife Saudável",
    status = "ok",
)
