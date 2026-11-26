package grupotp.medcall.server.repository

import grupotp.medcall.common.scheduling.AppointmentStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/** One appointment, reduced to what the KPIs read. No patient data besides an opaque id. */
data class AppointmentFact(
    val id: Int,
    val clinicId: Int,
    val professionalId: Int,
    val specialtyId: Int,
    val patientId: Int,
    val date: LocalDate,
    val time: LocalTime,
    val status: AppointmentStatus,
    val createdAt: Instant,
    val hasPreTriage: Boolean,
)

