package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.phone.Phone
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import grupotp.recifesaudavel.server.database.AppointmentsTable
import grupotp.recifesaudavel.server.database.ClinicsTable
import grupotp.recifesaudavel.server.database.PreTriagesTable
import grupotp.recifesaudavel.server.database.ProfessionalsTable
import grupotp.recifesaudavel.server.database.SchedulingCatalog
import grupotp.recifesaudavel.server.database.SpecialtiesTable
import grupotp.recifesaudavel.server.database.UsersTable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/** A professional, a date and a time: one slot of the agenda. */
data class Slot(
    val professionalId: String,
    val date: LocalDate,
    val time: LocalTime,
)

data class NewAppointment(
    val id: String,
    val patientId: String,
    val slot: Slot,
    val reason: String,
    val notes: String,
    val createdAt: Long,
)

class SchedulingRepository(
    private val database: Database,
) {
    fun seedCatalogIfEmpty() {
        transaction(database) {
            if (SpecialtiesTable.selectAll().count() > 0) {
                return@transaction
            }
            for (clinic in SchedulingCatalog.clinics) {
                ClinicsTable.insert {
                    it[id] = SchedulingCatalog.clinicId(clinic)
                    it[name] = clinic.name
                    it[city] = clinic.city
                    it[timeZone] = clinic.timeZoneId
                }
            }
            for ((specialty, professionals) in SchedulingCatalog.professionalsBySpecialty) {
                val specialtyId = SchedulingCatalog.specialtyId(specialty)
                SpecialtiesTable.insert {
                    it[id] = specialtyId
                    it[name] = specialty
                }
                for (professional in professionals) {
                    ProfessionalsTable.insert {
                        it[id] = SchedulingCatalog.professionalId(specialty, professional.name)
                        it[name] = professional.name
                        it[ProfessionalsTable.specialtyId] = specialtyId
                        it[clinicId] = SchedulingCatalog.clinicId(professional.clinic)
                    }
                }
            }
        }
    }

    fun specialties(): List<Specialty> = transaction(database) {
        SpecialtiesTable.selectAll()
            .orderBy(SpecialtiesTable.name)
            .map { Specialty(id = it[SpecialtiesTable.id], name = it[SpecialtiesTable.name]) }
    }

    fun professionals(specialtyId: String? = null): List<Professional> = transaction(database) {
        val query = ProfessionalsTable
            .join(ClinicsTable, JoinType.INNER, ProfessionalsTable.clinicId, ClinicsTable.id)
            .selectAll()
        if (specialtyId != null) {
            query.where { ProfessionalsTable.specialtyId eq specialtyId }
        }
        query.orderBy(ProfessionalsTable.name).map {
            Professional(
                id = it[ProfessionalsTable.id],
                name = it[ProfessionalsTable.name],
                specialtyId = it[ProfessionalsTable.specialtyId],
                clinic = it.toClinic(),
            )
        }
    }

    /** Slots already taken by appointments that were not cancelled. */
    fun takenSlots(professionalIds: List<String>, dates: List<LocalDate>): Set<Slot> = transaction(database) {
        if (professionalIds.isEmpty() || dates.isEmpty()) {
            return@transaction emptySet()
        }
        AppointmentsTable.selectAll()
            .where {
                (AppointmentsTable.professionalId inList professionalIds) and
                    (AppointmentsTable.appointmentDate inList dates.map { it.toString() }) and
                    (AppointmentsTable.status neq STATUS_CANCELLED)
            }
            .map {
                Slot(
                    professionalId = it[AppointmentsTable.professionalId],
                    date = LocalDate.parse(it[AppointmentsTable.appointmentDate]),
                    time = LocalTime.parse(it[AppointmentsTable.appointmentTime]),
                )
            }
            .toSet()
    }

    /**
     * Inserts the appointment. Returns false when the slot was taken in the meantime:
     * the unique slot key rejects the second insert even under concurrent requests.
     */
    fun insert(appointment: NewAppointment): Boolean = try {
        transaction(database) {
            AppointmentsTable.insert {
                it[id] = appointment.id
                it[patientId] = appointment.patientId
                it[professionalId] = appointment.slot.professionalId
                it[appointmentDate] = appointment.slot.date.toString()
                it[appointmentTime] = appointment.slot.time.toColumnText()
                it[status] = STATUS_PENDING
                it[reason] = appointment.reason
                it[notes] = appointment.notes
                it[createdAt] = appointment.createdAt
                it[slotKey] = AppointmentsTable.slotKeyOf(
                    appointment.slot.professionalId,
                    appointment.slot.date.toString(),
                    appointment.slot.time.toColumnText(),
                )
            }
        }
        true
    } catch (error: ExposedSQLException) {
        // Only a taken slot is a conflict; anything else is a server error and must surface as one.
        if (!error.isUniqueViolation()) {
            throw error
        }
        false
    }

    fun appointmentsOf(patientId: String): List<Appointment> = transaction(database) {
        appointmentQuery()
            .where { AppointmentsTable.patientId eq patientId }
            .orderBy(AppointmentsTable.appointmentDate to SortOrder.ASC, AppointmentsTable.appointmentTime to SortOrder.ASC)
            .map { it.toAppointment() }
    }

    /**
     * Marks the patient's own appointment as cancelled and clears its slot key, so the slot can
     * be booked again while the appointment stays in the history. Only changes it while its
     * status is still [expected], so a confirmation that lands in between is not overwritten.
     * Returns false when nothing was changed.
     */
    fun cancel(patientId: String, id: String, expected: AppointmentStatus): Boolean = transaction(database) {
        AppointmentsTable.update({
            (AppointmentsTable.id eq id) and
                (AppointmentsTable.patientId eq patientId) and
                (AppointmentsTable.status eq expected.stored())
        }) {
            it[status] = STATUS_CANCELLED
            it[slotKey] = null
        } > 0
    }

    /** The appointment when it belongs to [patientId], else null. */
    fun findAppointmentOf(patientId: String, id: String): Appointment? = transaction(database) {
        appointmentQuery()
            .where { (AppointmentsTable.id eq id) and (AppointmentsTable.patientId eq patientId) }
            .singleOrNull()
            ?.toAppointment()
    }

    private fun AppointmentStatus.stored(): String = when (this) {
        AppointmentStatus.Pending -> STATUS_PENDING
        AppointmentStatus.Confirmed -> STATUS_CONFIRMED
        AppointmentStatus.Cancelled -> STATUS_CANCELLED
    }

    fun findAppointment(id: String): Appointment? = transaction(database) {
        appointmentQuery()
            .where { AppointmentsTable.id eq id }
            .singleOrNull()
            ?.toAppointment()
    }

    private fun appointmentQuery() = AppointmentsTable
        .join(ProfessionalsTable, JoinType.INNER, AppointmentsTable.professionalId, ProfessionalsTable.id)
        .join(SpecialtiesTable, JoinType.INNER, ProfessionalsTable.specialtyId, SpecialtiesTable.id)
        .join(ClinicsTable, JoinType.INNER, ProfessionalsTable.clinicId, ClinicsTable.id)
        .join(UsersTable, JoinType.INNER, AppointmentsTable.patientId, UsersTable.id)
        .join(PreTriagesTable, JoinType.LEFT, AppointmentsTable.id, PreTriagesTable.appointmentId)
        .selectAll()

    private fun ResultRow.toAppointment(): Appointment = Appointment(
        id = this[AppointmentsTable.id],
        specialtyName = this[SpecialtiesTable.name],
        professionalName = this[ProfessionalsTable.name],
        clinic = toClinic(),
        date = LocalDate.parse(this[AppointmentsTable.appointmentDate]),
        time = LocalTime.parse(this[AppointmentsTable.appointmentTime]),
        status = when (this[AppointmentsTable.status]) {
            STATUS_CONFIRMED -> AppointmentStatus.Confirmed
            STATUS_CANCELLED -> AppointmentStatus.Cancelled
            else -> AppointmentStatus.Pending
        },
        patientName = this[UsersTable.socialName].ifBlank { this[UsersTable.fullName] },
        contactPhone = Phone(this[UsersTable.primaryPhone]),
        reason = this[AppointmentsTable.reason],
        notes = this[AppointmentsTable.notes],
        preTriageId = this.getOrNull(PreTriagesTable.id),
    )

    private fun ResultRow.toClinic(): Clinic = Clinic(
        id = this[ClinicsTable.id],
        name = this[ClinicsTable.name],
        city = this[ClinicsTable.city],
        timeZoneId = this[ClinicsTable.timeZone],
    )

    /** `HH:mm`, the column format: text order is time order, and old rows keep matching. */
    private fun LocalTime.toColumnText(): String =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    private companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_CANCELLED = "cancelled"
    }
}
