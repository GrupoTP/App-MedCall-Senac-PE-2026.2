package grupotp.medcall.common.kpi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/*
 * Answers of the KPI endpoints. The codes (C01, M03...) and formulas are the ones in
 * docs/kpis.md; change the catalog first.
 */

/**
 * The MedCall panel overview. Only aggregates: no appointment rows, no patient data.
 *
 * @property from first day of the period (UTC dates for the platform-wide counts).
 */
@Serializable
data class PlatformDashboard(
    val from: LocalDate,
    val to: LocalDate,
    val newPatients: Int,
    val newPatientsPrevious: Int,
    val patientsWhoBooked: Int,
    val patientsWhoBookedPrevious: Int,
    val appointments: Int,
    val appointmentsPrevious: Int,
    val activeClinics: Int,
    val totalClinics: Int,
    val medianConfirmationHours: Double?,
    val preTriageUsage: Rate,
    val confirmation: Rate,
    val noShow: Rate,
    val occupancy: Rate,
    val weekly: List<PlatformWeek>,
    val clinics: List<ClinicRanking>,
    val integrationModes: List<IntegrationModeCount>,
    val webhookDeliveries: Rate,
    val deadLetters: Int,
)

