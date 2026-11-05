package grupotp.recifesaudavel.server.domain

import kotlin.time.Instant

data class NewAppointment(
    val patientId: Int,
    val slot: Slot,
    val reason: String,
    val notes: String,
    val createdAt: Instant,
)
