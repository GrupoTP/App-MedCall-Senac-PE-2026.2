package grupotp.medcall.common.integration

import kotlinx.serialization.Serializable

@Serializable
data class SaveWebhookRequest(
    val url: String,
    val description: String = "",
    val events: List<WebhookEvent>,
    val active: Boolean = true,
)

