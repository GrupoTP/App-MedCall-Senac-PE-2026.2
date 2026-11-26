package grupotp.medcall.common.clinic

import kotlinx.serialization.Serializable

@Serializable
data class DeclineRequest(
    val reason: DeclineReason,
    val note: String = "",
)

