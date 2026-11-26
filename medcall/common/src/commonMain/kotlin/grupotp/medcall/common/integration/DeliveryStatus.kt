package grupotp.medcall.common.integration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DeliveryStatus {
    @SerialName("pending") Pending,
    @SerialName("delivered") Delivered,
    @SerialName("retrying") Retrying,
    @SerialName("dead") Dead,
}

