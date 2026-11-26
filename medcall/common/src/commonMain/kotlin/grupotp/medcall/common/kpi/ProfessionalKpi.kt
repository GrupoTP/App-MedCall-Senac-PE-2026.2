package grupotp.medcall.common.kpi

import kotlinx.serialization.Serializable

@Serializable
data class ProfessionalKpi(
    val professionalId: Int,
    val name: String,
    val specialtyName: String,
    val occupancy: Rate,
    val noShow: Rate,
    val medianConfirmationHours: Double?,
)

