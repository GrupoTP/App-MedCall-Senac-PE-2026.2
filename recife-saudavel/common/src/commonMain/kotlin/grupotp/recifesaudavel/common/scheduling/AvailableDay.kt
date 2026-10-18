package grupotp.recifesaudavel.common.scheduling

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * Free times of one day, as offered by the server.
 *
 * @property date clinic date, see [ClinicTime].
 * @property times clinic wall-clock times, in ascending order.
 */
@Serializable
data class AvailableDay(
    val date: LocalDate,
    val times: List<LocalTime>,
)
