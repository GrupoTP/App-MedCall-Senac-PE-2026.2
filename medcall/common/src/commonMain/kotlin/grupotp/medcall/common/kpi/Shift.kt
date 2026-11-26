package grupotp.medcall.common.kpi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Shift {
    @SerialName("morning") Morning,
    @SerialName("afternoon") Afternoon,
}

