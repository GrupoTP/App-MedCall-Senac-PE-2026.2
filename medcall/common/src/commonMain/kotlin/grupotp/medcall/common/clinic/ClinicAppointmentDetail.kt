package grupotp.medcall.common.clinic

import grupotp.medcall.common.pretriage.PreTriageForm
import kotlinx.serialization.Serializable

@Serializable
data class ClinicAppointmentDetail(
    val appointment: ClinicAppointment,
    val history: List<AppointmentHistoryEntry>,
    val preTriage: PreTriageForm? = null,
)

