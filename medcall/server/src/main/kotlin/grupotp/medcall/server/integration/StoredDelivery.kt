package grupotp.medcall.server.integration

import grupotp.medcall.common.integration.WebhookDelivery

data class StoredDelivery(
    val delivery: WebhookDelivery,
    val payload: String,
)

