package grupotp.medcall.common.integration

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Webhook(
    val id: Int,
    val url: String,
    val description: String = "",
    val events: List<WebhookEvent>,
    val active: Boolean,
    val createdAt: Instant,
    /** Set when the server switched it off after failing for 24 hours. */
    val disabledReason: String = "",
    val secretRotatedAt: Instant? = null,
    /** The clinic it belongs to; the Painel MedCall lists webhooks of every clinic. */
    val clinicId: Int = 0,
)

