package grupotp.medcall.server.integration

import grupotp.medcall.common.clinic.AppointmentEventType
import kotlin.time.Instant

/** An outbox row waiting for the broker. */
data class OutboxMessage(
    val id: Int,
    val eventId: String,
    val clinicId: Int,
    val appointmentId: Int,
    val eventType: AppointmentEventType,
    val occurredAt: Instant,
)

