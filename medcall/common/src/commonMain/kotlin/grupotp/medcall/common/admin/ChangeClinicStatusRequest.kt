package grupotp.medcall.common.admin

import grupotp.medcall.common.kpi.ClinicStatus
import kotlinx.serialization.Serializable

@Serializable
data class ChangeClinicStatusRequest(
    val status: ClinicStatus,
)

