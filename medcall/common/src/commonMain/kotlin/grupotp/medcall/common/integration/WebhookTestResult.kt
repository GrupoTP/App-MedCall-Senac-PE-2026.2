package grupotp.medcall.common.integration

import kotlinx.serialization.Serializable

/** Result of the "Enviar teste" button. */
@Serializable
data class WebhookTestResult(
    val statusCode: Int?,
    val durationMs: Long,
    val error: String = "",
)

