package grupotp.medcall.common.kpi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class IntegrationMode {
    @SerialName("manual") Manual,
    @SerialName("webhook") Webhook,
}

