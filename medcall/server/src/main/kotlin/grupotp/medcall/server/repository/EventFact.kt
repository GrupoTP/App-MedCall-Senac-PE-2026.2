package grupotp.medcall.server.repository

import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.clinic.EventOrigin
import kotlin.time.Instant

data class EventFact(
    val appointmentId: Int,
    val type: AppointmentEventType,
    val occurredAt: Instant,
    val origin: EventOrigin,
)

