package grupotp.medcall.common.integration

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.Serializable

/** A new webhook or a rotated secret. [secret] is shown once. */
@Serializable
data class WebhookWithSecret(
    val webhook: Webhook,
    val secret: SecretString,
)

