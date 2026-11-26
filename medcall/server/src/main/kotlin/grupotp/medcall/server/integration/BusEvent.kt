package grupotp.medcall.server.integration

import kotlinx.serialization.Serializable

/** What travels on the bus: an appointment event of a clinic. */
@Serializable
data class BusEvent(
    val eventId: String,
    val clinicId: Int,
    val appointmentId: Int,
    /** Wire name of AppointmentEventType. */
    val type: String,
    val occurredAtEpochMs: Long,
)

