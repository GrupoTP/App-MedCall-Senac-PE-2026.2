package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.common.AppointmentStatus
import kotlin.js.Date

private val MONTHS = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
private val WEEKDAYS = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
private val WEEKDAYS_LONG = listOf("domingo", "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado")

/** `2026-10-13` → `13/10/2026`. */
internal fun formatDate(isoDate: String): String {
    val (year, month, day) = isoDate.split("-")
    return "$day/$month/$year"
}

/** `2026-10-13` → `Ter, 13/10`. */
internal fun formatShortDate(isoDate: String): String {
    val (_, month, day) = isoDate.split("-")
    return "${WEEKDAYS[weekday(isoDate)]}, $day/$month"
}

/** Spoken form for screen readers: `terça-feira, 13/10`. */
internal fun formatSpokenDate(isoDate: String): String {
    val (_, month, day) = isoDate.split("-")
    return "${WEEKDAYS_LONG[weekday(isoDate)]}, $day/$month"
}

/** Epoch milliseconds → `13/10/2026 às 14:05`, in the device time zone. */
internal fun formatDateTime(epochMillis: Long): String {
    val date = Date(epochMillis.toDouble())
    return "${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} às " +
        "${pad(date.getHours())}:${pad(date.getMinutes())}"
}

internal fun dayOf(isoDate: String): String = isoDate.split("-")[2]

internal fun monthAbbreviation(isoDate: String): String = MONTHS[isoDate.split("-")[1].toInt() - 1]

/** Device local date and time as `yyyy-MM-ddTHH:mm`, comparable with appointment dates. */
internal fun localNow(): String {
    val now = Date()
    return "${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}"
}

internal fun AppointmentStatus.label(): String = when (this) {
    AppointmentStatus.Pending -> "Aguardando"
    AppointmentStatus.Confirmed -> "Confirmada"
    AppointmentStatus.Cancelled -> "Cancelada"
}

private fun weekday(isoDate: String): Int {
    val (year, month, day) = isoDate.split("-").map(String::toInt)
    return Date(year, month - 1, day).getDay()
}

private fun pad(value: Int): String = value.toString().padStart(2, '0')
