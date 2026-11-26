package grupotp.medcall.common.kpi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** C04 per week (Monday of the week, clinic time) of booking. */
@Serializable
data class WeeklyConfirmation(
    val weekStart: LocalDate,
    val medianHours: Double?,
    val answered: Int,
    val stillPending: Int,
)

