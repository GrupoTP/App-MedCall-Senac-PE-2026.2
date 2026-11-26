package grupotp.medcall.common.kpi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** C01 and C07 per clinic day. */
@Serializable
data class DailyPoint(
    val date: LocalDate,
    val received: Int,
    val attended: Int,
    val noShows: Int,
)

