package grupotp.medcall.common.kpi

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/*
 * Answers of the KPI endpoints. The codes (C01, M03...) and formulas are the ones in
 * docs/kpis.md; change the catalog first.
 */

/**
 * A ratio kept as its two counts, so the screen can show "12 de 40" next to the percentage and
 * a reviewer can recompute it.
 *
 * @property masked true when the MedCall panel hides a group under [MIN_GROUP] cases (LGPD); the
 * counts are then zero and [value] is null.
 */
@Serializable
data class Rate(
    val numerator: Int,
    val denominator: Int,
    val masked: Boolean = false,
) {
    /** 0.0 to 1.0, or null when there is nothing to divide by. */
    val value: Double? get() = if (masked || denominator == 0) null else numerator.toDouble() / denominator

    companion object {
        val EMPTY = Rate(0, 0)
        val MASKED = Rate(0, 0, masked = true)

        /** Smallest group the MedCall panel shows (docs/kpis.md, "Mascaramento"). */
        const val MIN_GROUP = 5
    }
}

@Serializable
enum class Shift {
    @SerialName("morning") Morning,
    @SerialName("afternoon") Afternoon,
}

/** C01 and C07 per clinic day. */
@Serializable
data class DailyPoint(
    val date: LocalDate,
    val received: Int,
    val attended: Int,
    val noShows: Int,
)

/** C02 per specialty and consultation day. */
@Serializable
data class OccupancyCell(
    val specialtyId: Int,
    val date: LocalDate,
    val taken: Int,
    val offered: Int,
)

/** C04 per week (Monday of the week, clinic time) of booking. */
@Serializable
data class WeeklyConfirmation(
    val weekStart: LocalDate,
    val medianHours: Double?,
    val answered: Int,
    val stillPending: Int,
)

/** C07 per weekday (1 = Monday) and shift of the consultation. */
@Serializable
data class NoShowCell(
    val dayOfWeek: Int,
    val shift: Shift,
    val noShows: Int,
    val marked: Int,
)

/** C09: active appointments per weekday (1 = Monday) and clinic time. */
@Serializable
data class PeakCell(
    val dayOfWeek: Int,
    val time: LocalTime,
    val count: Int,
)

@Serializable
data class ProfessionalKpi(
    val professionalId: Int,
    val name: String,
    val specialtyName: String,
    val occupancy: Rate,
    val noShow: Rate,
    val medianConfirmationHours: Double?,
)

@Serializable
data class SpecialtyKpi(
    val specialtyId: Int,
    val name: String,
    val occupancy: Rate,
    val received: Int,
)

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

@Serializable
enum class ClinicStatus {
    @SerialName("draft") Draft,
    @SerialName("active") Active,
    @SerialName("inactive") Inactive,
}

@Serializable
enum class IntegrationMode {
    @SerialName("manual") Manual,
    @SerialName("webhook") Webhook,
}

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

@Serializable
data class PlatformWeek(
    val weekStart: LocalDate,
    val newPatients: Int,
    val appointments: Int,
)

@Serializable
data class IntegrationModeCount(
    val mode: IntegrationMode,
    val clinics: Int,
)

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
