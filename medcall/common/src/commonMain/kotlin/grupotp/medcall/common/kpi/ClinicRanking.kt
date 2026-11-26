package grupotp.medcall.common.kpi

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** M03, M05 and the C KPIs of one clinic, in the MedCall panel's ranking. */
@Serializable
data class ClinicRanking(
    val clinicId: Int,
    val name: String,
    val city: String,
    val status: ClinicStatus,
    val integrationMode: IntegrationMode,
    /** Null when fewer than [Rate.MIN_GROUP]. */
    val appointments: Int?,
    val occupancy: Rate,
    val confirmation: Rate,
    val medianConfirmationHours: Double?,
    val noShow: Rate,
    val pendingOver24h: Int?,
    val lastAppointmentAt: Instant?,
)

