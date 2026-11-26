package grupotp.medcall.common.kpi

import kotlinx.serialization.Serializable

/** C07 per weekday (1 = Monday) and shift of the consultation. */
@Serializable
data class NoShowCell(
    val dayOfWeek: Int,
    val shift: Shift,
    val noShows: Int,
    val marked: Int,
)

