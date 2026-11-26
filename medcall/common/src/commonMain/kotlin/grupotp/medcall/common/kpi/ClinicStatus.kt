package grupotp.medcall.common.kpi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ClinicStatus {
    @SerialName("draft") Draft,
    @SerialName("active") Active,
    @SerialName("inactive") Inactive,
}

