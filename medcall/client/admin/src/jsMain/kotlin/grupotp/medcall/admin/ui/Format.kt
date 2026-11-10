package grupotp.medcall.admin.ui

import grupotp.medcall.common.kpi.Rate
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.staff.StaffRole
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Instant

/** Texts and number formats of the portals, in Brazilian Portuguese. */
object Format {
    private val months = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
    val WEEKDAYS = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    private val weekdaysLong = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")

    fun integer(value: Int): String = value.toString().reversed().chunked(3).joinToString(".").reversed()

    fun decimal(value: Double, digits: Int = 1): String {
        val factor = if (digits == 0) 1.0 else 10.0.let { base -> (1..digits).fold(1.0) { acc, _ -> acc * base } }
        val rounded = (value * factor).roundToInt() / factor
        val text = if (digits == 0) rounded.roundToInt().toString() else rounded.asDynamic().toFixed(digits) as String
        return text.replace('.', ',')
    }

    fun percent(rate: Rate): String = rate.value?.let { decimal(it * 100) + "%" } ?: "—"

    fun percent(value: Double?): String = value?.let { decimal(it * 100) + "%" } ?: "—"

    fun ratio(rate: Rate): String = when {
        rate.masked -> "menos de ${Rate.MIN_GROUP} casos"
        rate.denominator == 0 -> "sem dados no período"
        else -> "${integer(rate.numerator)} de ${integer(rate.denominator)}"
    }

    fun hours(value: Double?): String = when {
        value == null -> "—"
        value < 1 -> "${(value * 60).roundToInt()} min"
        else -> decimal(value) + " h"
    }

    fun days(value: Double?): String = value?.let { decimal(it) + " dias" } ?: "—"

    fun date(date: LocalDate): String = "${date.day.toString().padStart(2, '0')}/${date.month.ordinal.plus(1).toString().padStart(2, '0')}/${date.year}"

    fun shortDate(date: LocalDate): String = "${date.day.toString().padStart(2, '0')}/${months[date.month.ordinal]}"

    fun weekdayDate(date: LocalDate): String = "${weekdaysLong[date.dayOfWeek.isoDayNumber - 1]}, ${shortDate(date)}"

    fun time(time: LocalTime): String = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

    fun weekday(day: DayOfWeek): String = WEEKDAYS[day.isoDayNumber - 1]

    /** An instant in the browser's zone: who reads the portal is at the clinic or at MedCall. */
    fun dateTime(instant: Instant): String {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${shortDate(local.date)} ${time(local.time)}"
    }

    fun relative(instant: Instant, now: Instant): String {
        val minutes = (now - instant).inWholeMinutes
        return when {
            minutes < 1 -> "agora"
            minutes < 60 -> "há $minutes min"
            minutes < 48 * 60 -> "há ${minutes / 60} h"
            else -> "há ${minutes / 1440} dias"
        }
    }

    /** "+12% vs período anterior", or null when there is nothing to compare. */
    fun variation(current: Int, previous: Int): Pair<String, Boolean>? {
        if (previous == 0) return null
        val change = (current - previous).toDouble() / previous
        val sign = if (change >= 0) "+" else "−"
        return "$sign${decimal(abs(change) * 100, 0)}%" to (change >= 0)
    }

    fun status(status: AppointmentStatus, rescheduled: Boolean = false): String = when (status) {
        AppointmentStatus.Pending -> "Aguardando"
        AppointmentStatus.Confirmed -> if (rescheduled) "Remarcada" else "Confirmada"
        AppointmentStatus.Declined -> "Recusada"
        AppointmentStatus.Cancelled -> "Cancelada pelo paciente"
        AppointmentStatus.Attended -> "Compareceu"
        AppointmentStatus.NoShow -> "Faltou"
    }

    fun statusClass(status: AppointmentStatus): String = when (status) {
        AppointmentStatus.Pending -> "status-pending"
        AppointmentStatus.Confirmed -> "status-confirmed"
        AppointmentStatus.Declined -> "status-declined"
        AppointmentStatus.Cancelled -> "status-cancelled"
        AppointmentStatus.Attended -> "status-attended"
        AppointmentStatus.NoShow -> "status-no_show"
    }

    fun role(role: StaffRole): String = when (role) {
        StaffRole.MedcallAdmin -> "Equipe MedCall"
        StaffRole.ClinicAdmin -> "Administrador"
        StaffRole.Receptionist -> "Recepcionista"
    }
}
