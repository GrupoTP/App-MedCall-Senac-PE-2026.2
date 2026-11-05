package grupotp.recifesaudavel.common.health

import kotlinx.serialization.Serializable

/**
 * Health payload exchanged between the server and the web client.
 * The server builds the current value.
 */
@Serializable
data class HealthStatus(
    val service: String,
    val status: String,
)
