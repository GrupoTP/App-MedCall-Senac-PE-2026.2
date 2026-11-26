package grupotp.medcall.server.repository

import grupotp.medcall.common.scheduling.AppointmentStatus
import kotlinx.datetime.LocalDate

/** What the agenda list can be narrowed to. */
data class AgendaFilter(
    val from: LocalDate,
    val to: LocalDate,
    val statuses: Set<AppointmentStatus> = emptySet(),
    val professionalId: Int? = null,
    val specialtyId: Int? = null,
    /** Confirmed appointments already past with no attendance mark. */
    val attendanceMissing: Boolean = false,
)

