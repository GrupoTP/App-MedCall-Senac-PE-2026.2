package grupotp.recifesaudavel.server.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** A professional, a date and a time: one slot of the agenda. */
data class Slot(
    val professionalId: Int,
    val date: LocalDate,
    val time: LocalTime,
)
