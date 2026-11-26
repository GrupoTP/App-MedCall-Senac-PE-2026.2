package grupotp.medcall.common.clinic

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class RescheduleRequest(
    val date: LocalDate,
    val time: LocalTime,
)

