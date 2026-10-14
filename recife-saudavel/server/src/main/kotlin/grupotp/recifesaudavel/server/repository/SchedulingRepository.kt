package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.Appointment
import grupotp.recifesaudavel.common.AppointmentStatus
import grupotp.recifesaudavel.common.Professional
import grupotp.recifesaudavel.common.Specialty
import grupotp.recifesaudavel.server.database.AppointmentsTable
import grupotp.recifesaudavel.server.database.ProfessionalsTable
import grupotp.recifesaudavel.server.database.SchedulingCatalog
import grupotp.recifesaudavel.server.database.SpecialtiesTable
import grupotp.recifesaudavel.server.database.UsersTable
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
    val date: String,
    val time: String,
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
            for ((specialty, professionals) in SchedulingCatalog.professionalsBySpecialty) {
                val specialtyId = SchedulingCatalog.specialtyId(specialty)
                SpecialtiesTable.insert {
                    it[id] = specialtyId
                    it[name] = specialty
                }
                for (professional in professionals) {
                    ProfessionalsTable.insert {
                        it[id] = SchedulingCatalog.professionalId(specialty, professional)
                        it[name] = professional
                        it[ProfessionalsTable.specialtyId] = specialtyId
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
        val query = ProfessionalsTable.selectAll()
        if (specialtyId != null) {
            query.where { ProfessionalsTable.specialtyId eq specialtyId }
        }
        query.orderBy(ProfessionalsTable.name).map {
            Professional(
                id = it[ProfessionalsTable.id],
                name = it[ProfessionalsTable.name],
                specialtyId = it[ProfessionalsTable.specialtyId],
            )
        }
    }

    /** Slots already taken by appointments that were not cancelled. */
    fun takenSlots(professionalIds: List<String>, dates: List<String>): Set<Slot> = transaction(database) {
        if (professionalIds.isEmpty() || dates.isEmpty()) {
            return@transaction emptySet()
        }
        AppointmentsTable.selectAll()
            .where {
                (AppointmentsTable.professionalId inList professionalIds) and
                    (AppointmentsTable.appointmentDate inList dates) and
                    (AppointmentsTable.status neq STATUS_CANCELLED)
            }
            .map {
                Slot(
                    professionalId = it[AppointmentsTable.professionalId],
                    date = it[AppointmentsTable.appointmentDate],
                    time = it[AppointmentsTable.appointmentTime],
                )
            }
            .toSet()
    }

    /**
     * Inserts the appointment. Returns false when the slot was taken in the meantime:
     * the unique index rejects the second insert even under concurrent requests.
     */
    fun insert(appointment: NewAppointment): Boolean = try {
        transaction(database) {
            AppointmentsTable.insert {
                it[id] = appointment.id
                it[patientId] = appointment.patientId
                it[professionalId] = appointment.slot.professionalId
                it[appointmentDate] = appointment.slot.date
                it[appointmentTime] = appointment.slot.time
                it[status] = STATUS_PENDING
                it[reason] = appointment.reason
                it[notes] = appointment.notes
                it[createdAt] = appointment.createdAt
            }
        }
        true
    } catch (_: ExposedSQLException) {
        false
    }

    fun appointmentsOf(patientId: String): List<Appointment> = transaction(database) {
        appointmentQuery()
            .where { AppointmentsTable.patientId eq patientId }
            .orderBy(AppointmentsTable.appointmentDate to SortOrder.ASC, AppointmentsTable.appointmentTime to SortOrder.ASC)
            .map { it.toAppointment() }
    }

    /**
     * Marks the patient's own appointment as cancelled, freeing its slot. Returns false when the
     * appointment does not exist or belongs to someone else.
     */
    fun cancel(patientId: String, id: String): Boolean = transaction(database) {
        AppointmentsTable.update({ (AppointmentsTable.id eq id) and (AppointmentsTable.patientId eq patientId) }) {
            it[status] = STATUS_CANCELLED
        } > 0
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
        .join(UsersTable, JoinType.INNER, AppointmentsTable.patientId, UsersTable.id)
        .selectAll()

    private fun ResultRow.toAppointment(): Appointment = Appointment(
        id = this[AppointmentsTable.id],
        specialtyName = this[SpecialtiesTable.name],
        professionalName = this[ProfessionalsTable.name],
        date = this[AppointmentsTable.appointmentDate],
        time = this[AppointmentsTable.appointmentTime],
        status = when (this[AppointmentsTable.status]) {
            STATUS_CONFIRMED -> AppointmentStatus.Confirmed
            STATUS_CANCELLED -> AppointmentStatus.Cancelled
            else -> AppointmentStatus.Pending
        },
        patientName = this[UsersTable.socialName].ifBlank { this[UsersTable.fullName] },
        contactPhone = this[UsersTable.primaryPhone],
        reason = this[AppointmentsTable.reason],
        notes = this[AppointmentsTable.notes],
    )

    private companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_CANCELLED = "cancelled"
    }
}
