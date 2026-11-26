package grupotp.medcall.common.kpi

import kotlinx.serialization.Serializable

@Serializable
data class IntegrationModeCount(
    val mode: IntegrationMode,
    val clinics: Int,
)

