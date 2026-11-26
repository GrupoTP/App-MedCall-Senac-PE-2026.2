package grupotp.medcall.common.clinic

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** One status change, newest last. */
@Serializable
data class AppointmentHistoryEntry(
    val type: AppointmentEventType,
    val occurredAt: Instant,
    val origin: EventOrigin,
    val actorName: String = "",
    val detail: String = "",
)

