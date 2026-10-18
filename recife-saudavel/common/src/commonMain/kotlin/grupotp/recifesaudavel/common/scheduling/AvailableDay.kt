package grupotp.recifesaudavel.common.scheduling

import kotlinx.serialization.Serializable

/**
 * Free times of one day, as offered by the server.
 *
 * @property date ISO date, such as `2026-10-14`.
 * @property times `HH:mm` times, in ascending order.
 */
@Serializable
data class AvailableDay(
    val date: String,
    val times: List<String>,
)
