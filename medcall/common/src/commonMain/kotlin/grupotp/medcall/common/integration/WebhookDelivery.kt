package grupotp.medcall.common.integration

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class WebhookDelivery(
    val id: Int,
    val webhookId: Int,
    val eventId: String,
    val eventType: String,
    val status: DeliveryStatus,
    val attempts: Int,
    val lastStatusCode: Int? = null,
    val lastDurationMs: Long? = null,
    val lastError: String = "",
    val createdAt: Instant,
    val nextAttemptAt: Instant? = null,
)

