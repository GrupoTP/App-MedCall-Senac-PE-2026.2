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

@Serializable
data class SaveClinicRequest(
    val name: String,
    val city: String,
    val timeZoneId: String,
    val address: String = "",
    val phone: String = "",
)

@Serializable
data class ChangeClinicStatusRequest(
    val status: ClinicStatus,
)

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

@Serializable
data class SaveProfessionalRequest(
    val name: String,
    val registration: String = "",
    val specialtyId: Int,
    val clinicId: Int,
    val active: Boolean = true,
)

@Serializable
data class SaveSpecialtyRequest(
    val name: String,
)

/** One line of the audit log: who did what, never the data itself. */
@Serializable
data class AuditEntry(
    val occurredAt: Instant,
    val actorName: String,
    val action: String,
    val target: String,
    val clinicId: Int? = null,
)
