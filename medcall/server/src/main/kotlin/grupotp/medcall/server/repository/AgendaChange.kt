package grupotp.medcall.server.repository

import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.server.database.Actor
import grupotp.medcall.server.domain.Slot
import kotlin.time.Instant

/** One status change, applied only if the appointment is still as the caller read it. */
data class AgendaChange(
    val status: AppointmentStatus,
    val event: AppointmentEventType,
    val actor: Actor,
    val at: Instant,
    val detail: String = "",
    /** Where to move it; null keeps the slot. */
    val moveTo: Slot? = null,
)

