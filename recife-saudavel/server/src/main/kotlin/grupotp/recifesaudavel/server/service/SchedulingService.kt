package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import grupotp.recifesaudavel.common.scheduling.AvailableDay
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import grupotp.recifesaudavel.server.repository.NewAppointment
import grupotp.recifesaudavel.server.repository.PreTriageRepository
import grupotp.recifesaudavel.server.repository.SchedulingRepository
import grupotp.recifesaudavel.server.repository.Slot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.UUID

sealed interface BookingResult {
    data class Created(val appointment: Appointment) : BookingResult

    /** The request is invalid: unknown specialty, professional from another specialty, closed slot. */
    data class Rejected(val message: String) : BookingResult

    /** Valid request, but someone took the slot first. */
    data class Conflict(val message: String) : BookingResult
}

sealed interface CancelResult {
    data class Cancelled(val appointment: Appointment) : CancelResult

    /** No appointment with this id belongs to the patient. */
    data object NotFound : CancelResult

    /** Already cancelled, or confirmed and already started. */
    data class NotCancellable(val message: String) : CancelResult
}

/**
 * Agenda rules. Every slot the client shows comes from [availability], and [book] checks
 * the same rules again before writing (RN-009, RN-011).
 *
 * "Today" and "past times" are read in each clinic's own time zone ([Clinic.timeZoneId]), so the
 * zone of [clock] and of the server does not matter.
 */
class SchedulingService(
    private val repository: SchedulingRepository,
    private val clock: Clock,
    private val preTriages: PreTriageRepository,
) {
    fun specialties(): List<Specialty> = repository.specialties()

    fun professionals(specialtyId: String?): List<Professional> = repository.professionals(specialtyId)

    /**
     * Open days and times for the next [BOOKING_WINDOW_DAYS] days, one entry per clinic. A time is
     * open when the chosen professional, or any professional of the specialty at that clinic when
     * none is chosen, is free. Returns null when the specialty or the professional does not exist.
     */
    fun availability(specialtyId: String, professionalId: String?): List<ClinicAvailability>? {
        val candidates = candidates(specialtyId, professionalId) ?: return null
        return candidates.groupBy { it.clinic }.map { (clinic, atClinic) ->
            ClinicAvailability(clinic, openDays(clinic, atClinic))
        }
    }

    private fun openDays(clinic: Clinic, professionals: List<Professional>): List<AvailableDay> {
        val days = bookableDays(clinic)
        val taken = repository.takenSlots(professionals.map { it.id }, days)
        val now = nowAt(clinic)
        return days.mapNotNull { day ->
            val times = OPENING_TIMES.filter { time ->
                LocalDateTime(day, time) > now && professionals.any { Slot(it.id, day, time) !in taken }
            }
            if (times.isEmpty()) null else AvailableDay(date = day, times = times)
        }
    }

    fun book(patientId: String, request: CreateAppointmentRequest): BookingResult {
        val professionalId = request.professionalId?.takeIf { it.isNotBlank() }
        val clinicId = request.clinicId?.takeIf { it.isNotBlank() }
        val candidates = candidates(request.specialtyId, professionalId)
            ?.filter { clinicId == null || it.clinic.id == clinicId }
            ?.ifEmpty { null }
            ?: return BookingResult.Rejected("Especialidade, profissional ou clínica inválidos.")
        // The date and time only mean something in one clinic's zone.
        val clinic = candidates.map { it.clinic }.distinct().singleOrNull()
            ?: return BookingResult.Rejected("Escolha a clínica do horário.")
        val date = request.date
        if (date !in bookableDays(clinic)) {
            return BookingResult.Rejected("Escolha uma data disponível.")
        }
        if (request.time !in OPENING_TIMES || LocalDateTime(date, request.time) <= nowAt(clinic)) {
            return BookingResult.Rejected("Escolha um horário disponível.")
        }
        if (request.reason.length > MAX_TEXT || request.notes.length > MAX_TEXT) {
            return BookingResult.Rejected("Texto muito longo. Use até $MAX_TEXT caracteres.")
        }
        val preTriageId = request.preTriageId?.takeIf { it.isNotBlank() }
        if (preTriageId != null) {
            attachProblem(patientId, preTriageId)?.let { return BookingResult.Rejected(it) }
        }
        // The first free professional wins; if another patient takes it meanwhile, the unique
        // index refuses the insert and the next candidate is tried.
        val taken = repository.takenSlots(candidates.map { it.id }, listOf(date))
        for (professional in candidates) {
            val slot = Slot(professional.id, date, request.time)
            if (slot in taken) {
                continue
            }
            val id = UUID.randomUUID().toString()
            val inserted = repository.insert(
                NewAppointment(
                    id = id,
                    patientId = patientId,
                    slot = slot,
                    reason = request.reason.trim(),
                    notes = request.notes.trim(),
                    createdAt = clock.millis(),
                ),
            )
            if (inserted) {
                if (preTriageId != null) {
                    preTriages.attach(patientId, preTriageId, id)
                }
                return BookingResult.Created(repository.findAppointment(id)!!)
            }
        }
        return BookingResult.Conflict("Este horário acabou de ser ocupado. Escolha outro horário.")
    }

    /** Why the pre-triage cannot be attached, or null when it can. */
    private fun attachProblem(patientId: String, preTriageId: String): String? {
        val preTriage = preTriages.find(patientId, preTriageId) ?: return "Pré-triagem não encontrada."
        val current = preTriage.appointmentId?.let { repository.findAppointment(it) }
        return if (current != null && current.status != AppointmentStatus.Cancelled) {
            "Esta pré-triagem já está vinculada a outra consulta."
        } else {
            null
        }
    }

    fun appointmentsOf(patientId: String): List<Appointment> = repository.appointmentsOf(patientId)

    /** Cancels the patient's own appointment when [isCancellableAt] allows it. */
    fun cancel(patientId: String, appointmentId: String): CancelResult {
        val current = repository.findAppointmentOf(patientId, appointmentId) ?: return CancelResult.NotFound
        if (!current.isCancellableAt(nowAt(current.clinic)) || !repository.cancel(patientId, appointmentId, current.status)) {
            return CancelResult.NotCancellable("Esta consulta não pode mais ser cancelada.")
        }
        return CancelResult.Cancelled(repository.findAppointment(appointmentId)!!)
    }

    private fun candidates(specialtyId: String, professionalId: String?): List<Professional>? {
        val professionals = repository.professionals(specialtyId)
        if (professionals.isEmpty()) {
            return null
        }
        if (professionalId == null) {
            return professionals
        }
        return professionals.filter { it.id == professionalId }.ifEmpty { null }
    }

    /** Wall-clock date and time at [clinic] now. */
    private fun nowAt(clinic: Clinic): LocalDateTime =
        java.time.LocalDateTime.now(clock.withZone(ZoneId.of(clinic.timeZoneId))).toKotlinLocalDateTime()

    /** Weekdays from the clinic's today on. Clinics are closed on weekends. */
    private fun bookableDays(clinic: Clinic): List<LocalDate> {
        val today = java.time.LocalDate.now(clock.withZone(ZoneId.of(clinic.timeZoneId)))
        return (0 until BOOKING_WINDOW_DAYS)
            .map { today.plusDays(it.toLong()) }
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .map { it.toKotlinLocalDate() }
    }

    companion object {
        const val BOOKING_WINDOW_DAYS = 14
        const val MAX_TEXT = 1000

        /** Same times as the prototype. */
        val OPENING_TIMES: List<LocalTime> = listOf(
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00", "17:30",
        ).map(LocalTime::parse)
    }
}
