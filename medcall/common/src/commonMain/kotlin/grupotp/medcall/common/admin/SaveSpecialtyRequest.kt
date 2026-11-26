package grupotp.medcall.common.admin

import kotlinx.serialization.Serializable

@Serializable
data class SaveSpecialtyRequest(
    val name: String,
)

