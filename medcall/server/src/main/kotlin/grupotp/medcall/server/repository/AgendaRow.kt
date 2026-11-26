package grupotp.medcall.server.repository

import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.scheduling.Clinic
import grupotp.medcall.server.domain.Slot
import kotlin.time.Instant

/** What an action needs to know about an appointment before deciding. */
data class AgendaRow(
    val id: Int,
    val clinic: Clinic,
    val professionalId: Int,
    val slot: Slot,
    val status: AppointmentStatus,
    /** When the current attended/no-show mark was made. */
    val markedAt: Instant?,
)

