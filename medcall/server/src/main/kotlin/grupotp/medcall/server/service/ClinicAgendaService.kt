package grupotp.medcall.server.service

import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.clinic.ClinicAppointment
import grupotp.medcall.common.clinic.ClinicAppointmentDetail
import grupotp.medcall.common.clinic.ClinicCatalog
import grupotp.medcall.common.clinic.DeclineRequest
import grupotp.medcall.common.clinic.ProfessionalFreeDay
import grupotp.medcall.common.clinic.RescheduleRequest
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.scheduling.Clinic
import grupotp.medcall.server.database.Actor
import grupotp.medcall.server.domain.ServiceError
import grupotp.medcall.server.domain.Slot
import grupotp.medcall.server.repository.AgendaChange
import grupotp.medcall.server.repository.AgendaFilter
import grupotp.medcall.server.repository.AgendaRow
import grupotp.medcall.server.repository.ChangeOutcome
import grupotp.medcall.server.repository.ClinicAgendaRepository
import grupotp.medcall.server.repository.DeclineDetail
import grupotp.medcall.server.repository.SchedulingRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.time.Clock
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/** Answer of an agenda action. */
sealed interface AgendaResult {
    data class Done(val appointment: ClinicAppointment) : AgendaResult
    data object NotFound : AgendaResult
    data class Rejected(val error: ServiceError) : AgendaResult
    /** Someone changed the appointment since it was read: the screen should reload it. */
    data object Changed : AgendaResult
    data object SlotTaken : AgendaResult
}

/**
 * The clinic's side of an appointment: confirm, decline, reschedule and attendance. Used by the
 * clinic portal and by the public API, so both follow the same rules (see AppointmentStatus).
 *
 * "Now" is always read in the clinic's own time zone.
 */
class ClinicAgendaService(
    private val agenda: ClinicAgendaRepository,
    private val scheduling: SchedulingRepository,
    private val clock: Clock,
) {
    fun catalog(clinicId: Int): ClinicCatalog? = agenda.catalog(clinicId)

    fun list(clinicId: Int, filter: AgendaFilter): List<ClinicAppointment> {
        val today = today(clinicId) ?: return emptyList()
        // "Sem registro de comparecimento" only makes sense up to today.
        val effective = if (filter.attendanceMissing && filter.to > today) filter.copy(to = today) else filter
        val list = agenda.list(clinicId, effective, now())
        if (!filter.attendanceMissing) {
            return list
        }
        val clinic = agenda.catalog(clinicId)!!.clinic
        val nowAtClinic = nowAt(clinic)
        return list.filter { LocalDateTime(it.date, it.time) <= nowAtClinic }
    }

    fun pendingCount(clinicId: Int): Long = today(clinicId)?.let { agenda.pendingCount(clinicId, it) } ?: 0

    fun detail(clinicId: Int, id: Int): ClinicAppointmentDetail? = agenda.detail(clinicId, id)

    fun find(clinicId: Int, id: Int): ClinicAppointment? = agenda.findAppointment(clinicId, id)

    fun confirm(clinicId: Int, id: Int, actor: Actor): AgendaResult = act(clinicId, id) { row ->
        if (row.status != AppointmentStatus.Pending) {
            return@act invalid()
        }
        AgendaChange(AppointmentStatus.Confirmed, AppointmentEventType.Confirmed, actor, now())
    }

    fun decline(clinicId: Int, id: Int, request: DeclineRequest, actor: Actor): AgendaResult = act(clinicId, id) { row ->
        if (!row.isOpen()) {
            return@act invalid()
        }
        if (request.note.length > MAX_NOTE) {
            return@act Rejection(ServiceError(ErrorCode.TEXT_TOO_LONG, MAX_NOTE))
        }
        AgendaChange(
            AppointmentStatus.Declined,
            AppointmentEventType.Declined,
            actor,
            now(),
            detail = DeclineDetail.encode(request.reason, request.note),
        )
    }

    /** Moves to a free time of the same professional; the appointment ends up confirmed. */
    fun reschedule(clinicId: Int, id: Int, request: RescheduleRequest, actor: Actor): AgendaResult = act(clinicId, id) { row ->
        if (!row.isOpen()) {
            return@act invalid()
        }
        val target = Slot(row.professionalId, request.date, request.time)
        if (target == row.slot) {
            return@act Rejection(ServiceError(ErrorCode.TIME_UNAVAILABLE))
        }
        if (request.date !in bookableDays(row.clinic)) {
            return@act Rejection(ServiceError(ErrorCode.DATE_UNAVAILABLE))
        }
        if (request.time !in SchedulingService.OPENING_TIMES || LocalDateTime(request.date, request.time) <= nowAt(row.clinic)) {
            return@act Rejection(ServiceError(ErrorCode.TIME_UNAVAILABLE))
        }
        AgendaChange(AppointmentStatus.Confirmed, AppointmentEventType.Rescheduled, actor, now(), moveTo = target)
    }

    fun markAttendance(clinicId: Int, id: Int, attended: Boolean, actor: Actor): AgendaResult = act(clinicId, id) { row ->
        if (row.status != AppointmentStatus.Confirmed) {
            return@act invalid()
        }
        if (LocalDateTime(row.slot.date, row.slot.time) > nowAt(row.clinic)) {
            return@act Rejection(ServiceError(ErrorCode.ATTENDANCE_TOO_EARLY))
        }
        if (attended) {
            AgendaChange(AppointmentStatus.Attended, AppointmentEventType.Attended, actor, now())
        } else {
            AgendaChange(AppointmentStatus.NoShow, AppointmentEventType.NoShow, actor, now())
        }
    }

    /** Clears an attendance mark made less than 24 hours ago; the history keeps both events. */
    fun undoAttendance(clinicId: Int, id: Int, actor: Actor): AgendaResult = act(clinicId, id) { row ->
        if (row.status != AppointmentStatus.Attended && row.status != AppointmentStatus.NoShow) {
            return@act invalid()
        }
        val markedAt = row.markedAt
        if (markedAt == null || now() - markedAt > ClinicAgendaRepository.UNDO_WINDOW) {
            return@act Rejection(ServiceError(ErrorCode.ATTENDANCE_UNDO_EXPIRED))
        }
        AgendaChange(AppointmentStatus.Confirmed, AppointmentEventType.AttendanceCleared, actor, now())
    }

    /** Free times of one of the clinic's professionals over the booking window, for a reschedule. */
    fun freeDays(clinicId: Int, professionalId: Int): List<ProfessionalFreeDay>? {
        val catalog = agenda.catalog(clinicId) ?: return null
        if (catalog.professionals.none { it.id == professionalId }) {
            return null
        }
        val days = bookableDays(catalog.clinic)
        val taken = scheduling.takenSlots(listOf(professionalId), days)
        val now = nowAt(catalog.clinic)
        return days.mapNotNull { day ->
            val times = SchedulingService.OPENING_TIMES.filter { time ->
                LocalDateTime(day, time) > now && Slot(professionalId, day, time) !in taken
            }
            if (times.isEmpty()) null else ProfessionalFreeDay(day, times)
        }
    }

    fun today(clinicId: Int): LocalDate? = agenda.catalog(clinicId)?.clinic?.let { nowAt(it).date }

    /** A refusal decided while looking at the row. */
    private class Rejection(val error: ServiceError)

    private fun invalid() = Rejection(ServiceError(ErrorCode.APPOINTMENT_TRANSITION_INVALID))

    /** Reads the row, lets [decide] build the change or refuse it, and applies it if nobody got there first. */
    private inline fun act(clinicId: Int, id: Int, decide: (AgendaRow) -> Any): AgendaResult {
        val row = agenda.find(clinicId, id) ?: return AgendaResult.NotFound
        return when (val decision = decide(row)) {
            is Rejection -> AgendaResult.Rejected(decision.error)
            is AgendaChange -> when (agenda.change(clinicId, row, decision)) {
                ChangeOutcome.Applied -> AgendaResult.Done(agenda.findAppointment(clinicId, id)!!)
                ChangeOutcome.Changed -> AgendaResult.Changed
                ChangeOutcome.SlotTaken -> AgendaResult.SlotTaken
            }
            else -> error("Unexpected decision $decision")
        }
    }

    /** Pending, or confirmed and not started yet. */
    private fun AgendaRow.isOpen(): Boolean = when (status) {
        AppointmentStatus.Pending -> true
        AppointmentStatus.Confirmed -> LocalDateTime(slot.date, slot.time) > nowAt(clinic)
        else -> false
    }

    private fun bookableDays(clinic: Clinic): List<LocalDate> {
        val today = nowAt(clinic).date
        return (0 until SchedulingService.BOOKING_WINDOW_DAYS)
            .map { today.plus(DatePeriod(days = it)) }
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
    }

    private fun now(): Instant = clock.instant().toKotlinInstant()

    private fun nowAt(clinic: Clinic): LocalDateTime = now().toLocalDateTime(TimeZone.of(clinic.timeZoneId))

    private companion object {
        const val MAX_NOTE = 280
    }
}
