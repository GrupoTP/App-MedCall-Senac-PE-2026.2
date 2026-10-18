package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.common.scheduling.Clinic
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
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

/**
 * Time at the clinic and, when the device is in another zone, the same moment on the device:
 * `09:30 em Manaus · 10:30 no seu horário`. Just `09:30` when both clocks show the same.
 */
internal fun formatClinicTime(date: LocalDate, time: LocalTime, clinic: Clinic): String {
    val local = LocalDateTime(date, time).toInstant(clinic.timeZone).toLocalDateTime(TimeZone.currentSystemDefault())
    if (local.date == date && local.time == time) {
        return formatTime(time)
    }
    val yours = if (local.date == date) {
        formatTime(local.time)
    } else {
        "${formatTime(local.time)} de ${dayOf(local.date)}/${pad(local.date.month.number)}"
    }
    return "${formatTime(time)} em ${clinic.city} · $yours no seu horário"
}

/**
 * Warns that the times listed for [clinic] are not on the device's clock, such as
 * `Horários de Manaus: 1 h a menos que o horário do seu aparelho.` Null when both agree on [date].
 */
internal fun clinicZoneNote(clinic: Clinic, date: LocalDate): String? {
    val noon = LocalDateTime(date, LocalTime(12, 0)).toInstant(clinic.timeZone)
    val deviceAhead = TimeZone.currentSystemDefault().offsetAt(noon).totalSeconds - clinic.timeZone.offsetAt(noon).totalSeconds
    if (deviceAhead == 0) {
        return null
    }
    val minutes = abs(deviceAhead) / 60
    val amount = if (minutes % 60 == 0) "${minutes / 60} h" else "${minutes / 60} h ${minutes % 60} min"
    val relation = if (deviceAhead > 0) "a menos que" else "a mais que"
    return "Horários de ${clinic.city}: $amount $relation o horário do seu aparelho."
}

internal fun dayOf(date: LocalDate): String = pad(date.day)

internal fun monthAbbreviation(date: LocalDate): String = MONTHS[date.month.number - 1]

private fun pad(value: Int): String = value.toString().padStart(2, '0')
