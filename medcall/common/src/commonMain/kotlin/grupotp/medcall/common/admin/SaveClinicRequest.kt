package grupotp.medcall.common.admin

import kotlinx.serialization.Serializable

@Serializable
data class SaveClinicRequest(
    val name: String,
    val city: String,
    val timeZoneId: String,
    val address: String = "",
    val phone: String = "",
)

