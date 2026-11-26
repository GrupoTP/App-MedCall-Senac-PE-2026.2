package grupotp.medcall.common.clinic

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/** Free times of one professional, for a reschedule. */
@Serializable
data class ProfessionalFreeDay(
    val date: LocalDate,
    val times: List<LocalTime>,
)

