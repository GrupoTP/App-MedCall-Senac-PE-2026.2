package grupotp.medcall.server.repository

import grupotp.medcall.common.account.Phone
import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.clinic.AppointmentHistoryEntry
import grupotp.medcall.common.clinic.ClinicAppointment
import grupotp.medcall.common.clinic.ClinicAppointmentDetail
import grupotp.medcall.common.clinic.ClinicCatalog
import grupotp.medcall.common.pretriage.PreTriageForm
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.scheduling.Clinic
import grupotp.medcall.common.scheduling.Professional
import grupotp.medcall.common.scheduling.Specialty
import grupotp.medcall.server.database.Actor
import grupotp.medcall.server.database.recordAppointmentEvent
import grupotp.medcall.server.database.table.ApiKeysTable
import grupotp.medcall.server.database.table.AppointmentEventsTable
import grupotp.medcall.server.database.table.AppointmentsTable
import grupotp.medcall.server.database.table.ClinicsTable
import grupotp.medcall.server.database.table.PreTriagesTable
import grupotp.medcall.server.database.table.ProfessionalsTable
import grupotp.medcall.server.database.table.SpecialtiesTable
import grupotp.medcall.server.database.table.StaffUsersTable
import grupotp.medcall.server.database.table.UsersTable
import grupotp.medcall.server.database.wireName
import grupotp.medcall.server.database.wireValue
import grupotp.medcall.server.domain.Slot
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** What the agenda list can be narrowed to. */
data class AgendaFilter(
    val from: LocalDate,
    val to: LocalDate,
    val statuses: Set<AppointmentStatus> = emptySet(),
    val professionalId: Int? = null,
    val specialtyId: Int? = null,
    /** Confirmed appointments already past with no attendance mark. */
    val attendanceMissing: Boolean = false,
)

/** What an action needs to know about an appointment before deciding. */
data class AgendaRow(
    val id: Int,
    val clinic: Clinic,
    val professionalId: Int,
    val slot: Slot,
    val status: AppointmentStatus,
    /** When the current attended/no-show mark was made. */
    val markedAt: Instant?,
)

/** One status change, applied only if the appointment is still as the caller read it. */
data class AgendaChange(
    val status: AppointmentStatus,
    val event: AppointmentEventType,
    val actor: Actor,
    val at: Instant,
    val detail: String = "",
    /** Where to move it; null keeps the slot. */
    val moveTo: Slot? = null,
)

sealed interface ChangeOutcome {
    data object Applied : ChangeOutcome
    /** Someone changed it since it was read. */
    data object Changed : ChangeOutcome
    /** The new slot of a reschedule was taken. */
    data object SlotTaken : ChangeOutcome
}

/** The agenda of one clinic. Every query takes the clinic id, so one clinic never reads another's rows. */
class ClinicAgendaRepository(
    private val database: Database,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun catalog(clinicId: Int): ClinicCatalog? = transaction(database) {
        val clinic = ClinicsTable.selectAll().where { ClinicsTable.id eq clinicId }.singleOrNull()?.toClinic()
            ?: return@transaction null
        val professionals = ProfessionalsTable.selectAll().where { ProfessionalsTable.clinic eq clinicId }
            .orderBy(ProfessionalsTable.name to SortOrder.ASC)
            .map {
                Professional(
                    id = it[ProfessionalsTable.id].value,
                    name = it[ProfessionalsTable.name],
                    specialtyId = it[ProfessionalsTable.specialty].value,
                    clinicId = clinicId,
                )
            }
        val specialtyIds = professionals.map { it.specialtyId }.distinct()
        val specialties = SpecialtiesTable.selectAll().where { SpecialtiesTable.id inList specialtyIds }
            .orderBy(SpecialtiesTable.name to SortOrder.ASC)
            .map { Specialty(it[SpecialtiesTable.id].value, it[SpecialtiesTable.name]) }
        ClinicCatalog(clinic, specialties, professionals)
    }

    fun list(clinicId: Int, filter: AgendaFilter, now: Instant): List<ClinicAppointment> = transaction(database) {
        var condition: Op<Boolean> = (ProfessionalsTable.clinic eq clinicId) and
            AppointmentsTable.date.between(filter.from, filter.to)
        if (filter.statuses.isNotEmpty()) {
            condition = condition and (AppointmentsTable.status inList filter.statuses.map { it.stored() })
        }
        filter.professionalId?.let { condition = condition and (AppointmentsTable.professional eq it) }
        filter.specialtyId?.let { condition = condition and (ProfessionalsTable.specialty eq it) }
        if (filter.attendanceMissing) {
            condition = condition and (AppointmentsTable.status eq AppointmentStatus.Confirmed.stored())
        }
        val rows = joined().where(condition)
            .orderBy(AppointmentsTable.date to SortOrder.ASC, AppointmentsTable.time to SortOrder.ASC, ProfessionalsTable.name to SortOrder.ASC)
            .toList()
        toAppointments(rows)
    }

    /** Pending appointments of the clinic that have not started yet. */
    fun pendingCount(clinicId: Int, today: LocalDate): Long = transaction(database) {
        AppointmentsTable.innerJoin(ProfessionalsTable).selectAll()
            .where {
                (ProfessionalsTable.clinic eq clinicId) and
                    (AppointmentsTable.status eq AppointmentStatus.Pending.stored()) and
                    AppointmentsTable.date.between(today, LocalDate(9999, 12, 31))
            }
            .count()
    }

    fun find(clinicId: Int, id: Int): AgendaRow? = transaction(database) {
        val row = AppointmentsTable.innerJoin(ProfessionalsTable).innerJoin(ClinicsTable).selectAll()
            .where { (AppointmentsTable.id eq id) and (ProfessionalsTable.clinic eq clinicId) }
            .singleOrNull() ?: return@transaction null
        AgendaRow(
            id = id,
            clinic = row.toClinic(),
            professionalId = row[AppointmentsTable.professional].value,
            slot = Slot(row[AppointmentsTable.professional].value, row[AppointmentsTable.date], row[AppointmentsTable.time]),
            status = storedStatus(row[AppointmentsTable.status]),
            markedAt = lastMarks(listOf(id))[id],
        )
    }

    fun findAppointment(clinicId: Int, id: Int): ClinicAppointment? = transaction(database) {
        joined().where { (AppointmentsTable.id eq id) and (ProfessionalsTable.clinic eq clinicId) }
            .singleOrNull()
            ?.let { toAppointments(listOf(it)).single() }
    }

    fun detail(clinicId: Int, id: Int): ClinicAppointmentDetail? = transaction(database) {
        val appointment = findAppointment(clinicId, id) ?: return@transaction null
        val history = AppointmentEventsTable
            .join(StaffUsersTable, JoinType.LEFT, AppointmentEventsTable.staffUser, StaffUsersTable.id)
            .join(ApiKeysTable, JoinType.LEFT, AppointmentEventsTable.apiKey, ApiKeysTable.id)
            .selectAll()
            .where { AppointmentEventsTable.appointment eq id }
            .orderBy(AppointmentEventsTable.occurredAt to SortOrder.ASC, AppointmentEventsTable.id to SortOrder.ASC)
            .map { row ->
                val type = wireValue<AppointmentEventType>(row[AppointmentEventsTable.type])
                AppointmentHistoryEntry(
                    type = type,
                    occurredAt = row[AppointmentEventsTable.occurredAt],
                    origin = wireValue(row[AppointmentEventsTable.origin]),
                    actorName = row.getOrNull(StaffUsersTable.fullName)
                        ?: row.getOrNull(ApiKeysTable.name)?.let { "Sistema da clínica ($it)" }
                        ?: "",
                    detail = when (type) {
                        AppointmentEventType.Declined -> DeclineDetail.text(row[AppointmentEventsTable.detail])
                        AppointmentEventType.Rescheduled -> {
                            val date = row[AppointmentEventsTable.previousDate]
                            val time = row[AppointmentEventsTable.previousTime]
                            if (date != null && time != null) "Antes: $date às $time" else ""
                        }
                        else -> row[AppointmentEventsTable.detail]
                    },
                )
            }
        val form = PreTriagesTable.select(PreTriagesTable.form)
            .where { PreTriagesTable.appointment eq id }
            .orderBy(PreTriagesTable.id to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let { json.decodeFromString<PreTriageForm>(it[PreTriagesTable.form]) }
        ClinicAppointmentDetail(appointment, history, form)
    }

    /**
     * Applies [change] if the appointment is still in [expected] (status and slot): the row is
     * locked while checking, so of two concurrent actions only the first wins.
     */
    fun change(clinicId: Int, expected: AgendaRow, change: AgendaChange): ChangeOutcome = try {
        transaction(database) {
            val current = AppointmentsTable.innerJoin(ProfessionalsTable).selectAll()
                .where { (AppointmentsTable.id eq expected.id) and (ProfessionalsTable.clinic eq clinicId) }
                .forUpdate()
                .singleOrNull()
            val unchanged = current != null &&
                current[AppointmentsTable.status] == expected.status.stored() &&
                current[AppointmentsTable.date] == expected.slot.date &&
                current[AppointmentsTable.time] == expected.slot.time
            if (!unchanged) {
                return@transaction ChangeOutcome.Changed
            }
            val target = change.moveTo ?: expected.slot
            AppointmentsTable.update({ AppointmentsTable.id eq expected.id }) {
                it[status] = change.status.stored()
                it[date] = target.date
                it[time] = target.time
                it[slotKey] = if (change.status.holdsSlot) {
                    AppointmentsTable.slotKeyOf(target.professionalId, target.date, target.time)
                } else {
                    null
                }
            }
            recordAppointmentEvent(
                appointmentId = expected.id,
                clinicId = clinicId,
                type = change.event,
                actor = change.actor,
                at = change.at,
                detail = change.detail,
                previous = if (change.moveTo != null) expected.slot else null,
            )
            ChangeOutcome.Applied
        }
    } catch (error: ExposedSQLException) {
        if (!error.isUniqueViolation()) {
            throw error
        }
        ChangeOutcome.SlotTaken
    }

    private fun joined() = AppointmentsTable
        .innerJoin(ProfessionalsTable)
        .innerJoin(SpecialtiesTable)
        .innerJoin(UsersTable)
        .selectAll()

    /** Rows of [joined] to the agenda's DTO, with what the events and pre-triages add. */
    private fun toAppointments(rows: List<ResultRow>): List<ClinicAppointment> {
        val ids = rows.map { it[AppointmentsTable.id].value }
        val extras = appointmentExtras(ids)
        val marks = lastMarks(ids)
        val withPreTriage = mutableSetOf<Int>()
        ids.chunked(IN_LIST_CHUNK).forEach { chunk ->
            PreTriagesTable.select(PreTriagesTable.appointment)
                .where { (PreTriagesTable.appointment inList chunk) and PreTriagesTable.appointment.isNotNull() }
                .forEach { row -> row[PreTriagesTable.appointment]?.value?.let(withPreTriage::add) }
        }
        return rows.map { row ->
            val id = row[AppointmentsTable.id].value
            val status = storedStatus(row[AppointmentsTable.status])
            val extra = extras[id]
            ClinicAppointment(
                id = id,
                date = row[AppointmentsTable.date],
                time = row[AppointmentsTable.time],
                status = status,
                patientName = row[UsersTable.socialName].ifBlank { row[UsersTable.fullName] },
                patientPhone = Phone.parse(row[UsersTable.primaryPhone]),
                specialtyId = row[ProfessionalsTable.specialty].value,
                specialtyName = row[SpecialtiesTable.name],
                professionalId = row[AppointmentsTable.professional].value,
                professionalName = row[ProfessionalsTable.name],
                reason = row[AppointmentsTable.reason],
                notes = row[AppointmentsTable.notes],
                createdAt = row[AppointmentsTable.createdAt],
                rescheduled = extra?.rescheduled ?: false,
                declineReason = extra?.declineReason.orEmpty(),
                hasPreTriage = id in withPreTriage,
                attendanceUndoUntil = if (status == AppointmentStatus.Attended || status == AppointmentStatus.NoShow) {
                    marks[id]?.plus(UNDO_WINDOW)
                } else {
                    null
                },
            )
        }
    }

    /** When each appointment got its latest attended or no-show mark. */
    private fun lastMarks(ids: List<Int>): Map<Int, Instant> {
        val types = listOf(AppointmentEventType.Attended.wireName(), AppointmentEventType.NoShow.wireName())
        val result = mutableMapOf<Int, Instant>()
        ids.chunked(IN_LIST_CHUNK).forEach { chunk ->
            AppointmentEventsTable.select(AppointmentEventsTable.appointment, AppointmentEventsTable.occurredAt)
                .where { (AppointmentEventsTable.appointment inList chunk) and (AppointmentEventsTable.type inList types) }
                .forEach { row ->
                    val id = row[AppointmentEventsTable.appointment].value
                    val at = row[AppointmentEventsTable.occurredAt]
                    if (result[id]?.let { at > it } != false) result[id] = at
                }
        }
        return result
    }

    private fun ResultRow.toClinic() = Clinic(
        id = this[ClinicsTable.id].value,
        name = this[ClinicsTable.name],
        city = this[ClinicsTable.city],
        timeZoneId = this[ClinicsTable.timeZone],
    )

    companion object {
        /** How long an attendance mark can be undone. */
        val UNDO_WINDOW = 24.hours
    }
}
