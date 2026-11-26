package grupotp.medcall.common.kpi

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/** C09: active appointments per weekday (1 = Monday) and clinic time. */
@Serializable
data class PeakCell(
    val dayOfWeek: Int,
    val time: LocalTime,
    val count: Int,
)

