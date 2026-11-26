package grupotp.medcall.server.integration

import grupotp.medcall.common.integration.Webhook
import kotlin.time.Instant

/** A webhook with what only the server reads. */
data class StoredWebhook(
    val webhook: Webhook,
    val clinicId: Int,
    val secret: String,
    val previousSecret: String?,
    val previousSecretUntil: Instant?,
    val failingSince: Instant?,
)

