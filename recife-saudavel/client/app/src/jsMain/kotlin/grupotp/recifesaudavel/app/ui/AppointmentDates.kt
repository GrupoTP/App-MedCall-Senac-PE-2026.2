package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val MONTHS = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")

/** Indexed by ISO day number - 1: Monday first. */
private val WEEKDAYS = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
private val WEEKDAYS_LONG = listOf("segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado", "domingo")

/** `13/10/2026`. */
internal fun formatDate(date: LocalDate): String = "${dayOf(date)}/${pad(date.month.number)}/${date.year}"

/** `Ter, 13/10`. */
internal fun formatShortDate(date: LocalDate): String =
    "${WEEKDAYS[date.dayOfWeek.isoDayNumber - 1]}, ${dayOf(date)}/${pad(date.month.number)}"

/** Spoken form for screen readers: `terça-feira, 13/10`. */
internal fun formatSpokenDate(date: LocalDate): String =
    "${WEEKDAYS_LONG[date.dayOfWeek.isoDayNumber - 1]}, ${dayOf(date)}/${pad(date.month.number)}"

/** `09:30`. Appointment times are clinic times and are shown as they are. */
internal fun formatTime(time: LocalTime): String = "${pad(time.hour)}:${pad(time.minute)}"

/**
 * `13/10/2026 às 14:05`, in the device time zone: a moment the person lived, such as their
 * last sign-in, reads in their own clock.
 */
internal fun formatDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${formatDate(local.date)} às ${formatTime(local.time)}"
}

internal fun dayOf(date: LocalDate): String = pad(date.day)

internal fun monthAbbreviation(date: LocalDate): String = MONTHS[date.month.number - 1]

internal fun AppointmentStatus.label(): String = when (this) {
    AppointmentStatus.Pending -> "Aguardando"
    AppointmentStatus.Confirmed -> "Confirmada"
    AppointmentStatus.Cancelled -> "Cancelada"
}

private fun pad(value: Int): String = value.toString().padStart(2, '0')
