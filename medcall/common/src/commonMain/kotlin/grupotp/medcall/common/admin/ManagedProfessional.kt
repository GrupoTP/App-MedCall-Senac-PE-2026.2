package grupotp.medcall.common.admin

import kotlinx.serialization.Serializable

@Serializable
data class ManagedProfessional(
    val id: Int,
    val name: String,
    val registration: String = "",
    val specialtyId: Int,
    val specialtyName: String,
    val clinicId: Int,
    val active: Boolean,
)

