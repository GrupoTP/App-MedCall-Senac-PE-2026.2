package grupotp.medcall.common.admin

import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.kpi.IntegrationMode
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** A clinic as the MedCall panel manages it. */
@Serializable
data class ManagedClinic(
    val id: Int,
    val name: String,
    val city: String,
    val timeZoneId: String,
    val address: String = "",
    val phone: String = "",
    val status: ClinicStatus,
    val integrationMode: IntegrationMode,
    val professionals: Int,
    val admins: Int,
    val lastAppointmentAt: Instant? = null,
)

