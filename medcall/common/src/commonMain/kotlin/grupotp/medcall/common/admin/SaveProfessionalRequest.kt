package grupotp.medcall.common.admin

import kotlinx.serialization.Serializable

@Serializable
data class SaveProfessionalRequest(
    val name: String,
    val registration: String = "",
    val specialtyId: Int,
    val clinicId: Int,
    val active: Boolean = true,
)

