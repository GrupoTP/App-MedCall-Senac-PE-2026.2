package grupotp.medcall.common.kpi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** C02 per specialty and consultation day. */
@Serializable
data class OccupancyCell(
    val specialtyId: Int,
    val date: LocalDate,
    val taken: Int,
    val offered: Int,
)

