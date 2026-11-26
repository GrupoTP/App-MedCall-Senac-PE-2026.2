package grupotp.medcall.common.kpi

import kotlinx.serialization.Serializable

@Serializable
data class SpecialtyKpi(
    val specialtyId: Int,
    val name: String,
    val occupancy: Rate,
    val received: Int,
)

