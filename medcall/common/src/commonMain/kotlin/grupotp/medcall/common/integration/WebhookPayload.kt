package grupotp.medcall.common.integration

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * The body MedCall posts to a clinic's webhook. Delivery is at least once: the clinic drops
 * repeats by [eventId].
 */
@Serializable
data class WebhookPayload(
    val eventId: String,
    val type: String,
    val occurredAt: Instant,
    val clinicId: Int,
    val appointment: PublicAppointment,
)

