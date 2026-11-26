package grupotp.medcall.common.kpi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/*
 * Answers of the KPI endpoints. The codes (C01, M03...) and formulas are the ones in
 * docs/kpis.md; change the catalog first.
 */

/**
 * The clinic dashboard (Portal da Clínica). Every block follows the same filters.
 *
 * @property from first day of the period, clinic time.
 * @property to last day of the period, inclusive.
 */
@Serializable
data class ClinicDashboard(
    val from: LocalDate,
    val to: LocalDate,
    val received: Int,
    val receivedPrevious: Int,
    val occupancy: Rate,
    val confirmation: Rate,
    val medianConfirmationHours: Double?,
    val pendingOver24h: Int,
    val patientCancellation: Rate,
    val noShow: Rate,
    val attendanceCoverage: Rate,
    val medianLeadDays: Double?,
    val daily: List<DailyPoint>,
    val occupancyBySpecialtyDay: List<OccupancyCell>,
    val confirmationByWeek: List<WeeklyConfirmation>,
    val noShowByWeekdayShift: List<NoShowCell>,
    val peak: List<PeakCell>,
    val professionals: List<ProfessionalKpi>,
    val specialties: List<SpecialtyKpi>,
)

