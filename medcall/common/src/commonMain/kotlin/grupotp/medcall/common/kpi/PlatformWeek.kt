package grupotp.medcall.common.kpi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class PlatformWeek(
    val weekStart: LocalDate,
    val newPatients: Int,
    val appointments: Int,
)

