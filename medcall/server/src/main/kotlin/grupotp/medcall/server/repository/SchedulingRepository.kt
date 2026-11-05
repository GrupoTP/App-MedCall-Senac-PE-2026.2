package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import grupotp.recifesaudavel.server.database.SchedulingCatalog
import grupotp.recifesaudavel.server.database.entity.AppointmentEntity
import grupotp.recifesaudavel.server.database.entity.ClinicEntity
import grupotp.recifesaudavel.server.database.entity.ProfessionalEntity
import grupotp.recifesaudavel.server.database.entity.SpecialtyEntity
import grupotp.recifesaudavel.server.database.entity.UserEntity
import grupotp.recifesaudavel.server.database.table.AppointmentsTable
import grupotp.recifesaudavel.server.database.table.ClinicsTable
import grupotp.recifesaudavel.server.database.table.ProfessionalsTable
import grupotp.recifesaudavel.server.database.table.SpecialtiesTable
import grupotp.recifesaudavel.server.domain.NewAppointment
import grupotp.recifesaudavel.server.domain.Slot
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SchedulingRepository(
    private val database: Database,
) {
    fun seedCatalogIfEmpty() {
        transaction(database) {
            if (!SpecialtyEntity.all().empty()) {
                return@transaction
            }
            val clinics = SchedulingCatalog.clinics.associateWith { clinic ->
                ClinicEntity.new {
                    name = clinic.name
                    city = clinic.city
                    timeZone = clinic.timeZoneId
                }
            }
            for ((specialtyName, professionals) in SchedulingCatalog.professionalsBySpecialty) {
                val specialty = SpecialtyEntity.new { name = specialtyName }
                for (professional in professionals) {
                    ProfessionalEntity.new {
                        name = professional.name
                        this.specialty = specialty
                        clinic = clinics.getValue(professional.clinic)
                    }
                }
            }
        }
    }

    fun specialties(): List<Specialty> = transaction(database) {
        SpecialtyEntity.all()
            .orderBy(SpecialtiesTable.name to SortOrder.ASC)
            .map { Specialty(id = it.id.value, name = it.name) }
    }

    fun clinics(): List<Clinic> = transaction(database) {
        ClinicEntity.all().orderBy(ClinicsTable.name to SortOrder.ASC).map { it.toClinic() }
    }

    fun findClinic(id: Int): Clinic? = transaction(database) {
        ClinicEntity.findById(id)?.toClinic()
    }

    fun professionals(specialtyId: Int? = null): List<Professional> = transaction(database) {
        val professionals = if (specialtyId == null) {
            ProfessionalEntity.all()
        } else {
            ProfessionalEntity.find { ProfessionalsTable.specialty eq specialtyId }
        }
        professionals.orderBy(ProfessionalsTable.name to SortOrder.ASC).map {
            Professional(
                id = it.id.value,
                name = it.name,
                specialtyId = it.specialtyId.value,
                clinicId = it.clinicId.value,
            )
        }
    }

    /** Slots already taken by appointments that were not cancelled. */
    fun takenSlots(professionalIds: List<Int>, dates: List<LocalDate>): Set<Slot> = transaction(database) {
        if (professionalIds.isEmpty() || dates.isEmpty()) {
            return@transaction emptySet()
        }
        AppointmentEntity.find {
            (AppointmentsTable.professional inList professionalIds) and
                (AppointmentsTable.date inList dates) and
                (AppointmentsTable.status neq STATUS_CANCELLED)
        }.mapTo(mutableSetOf()) { Slot(it.professionalId.value, it.date, it.time) }
    }

    /**
     * Inserts the appointment and returns its id. Returns null when the slot was taken in the
     * meantime: the unique slot key rejects the second insert even under concurrent requests.
     */
    fun insert(appointment: NewAppointment): Int? = try {
        transaction(database) {
            val slot = appointment.slot
            AppointmentEntity.new {
                patient = UserEntity[appointment.patientId]
                professional = ProfessionalEntity[slot.professionalId]
                date = slot.date
                time = slot.time
                status = STATUS_PENDING
                reason = appointment.reason
                notes = appointment.notes
                createdAt = appointment.createdAt
                slotKey = AppointmentsTable.slotKeyOf(slot.professionalId, slot.date, slot.time)
            }.id.value
        }
    } catch (error: ExposedSQLException) {
        // Only a taken slot is a conflict; anything else is a server error and must surface as one.
        if (!error.isUniqueViolation()) {
            throw error
        }
        null
    }

    fun appointmentsOf(patientId: Int): List<Appointment> = transaction(database) {
        AppointmentEntity.find { AppointmentsTable.patient eq patientId }
            .orderBy(AppointmentsTable.date to SortOrder.ASC, AppointmentsTable.time to SortOrder.ASC)
            .withDetails()
            .map { it.toAppointment() }
    }

    /**
     * Marks the patient's own appointment as cancelled and clears its slot key, so the slot can
     * be booked again while the appointment stays in the history. Only changes it while its
     * status is still [expected]: the row is locked when read, so a confirmation that lands in
     * between is not overwritten. Returns false when nothing was changed.
     */
    fun cancel(patientId: Int, id: Int, expected: AppointmentStatus): Boolean = transaction(database) {
        val appointment = AppointmentEntity
            .find { ownedBy(patientId, id) and (AppointmentsTable.status eq expected.stored()) }
            .forUpdate()
            .singleOrNull()
            ?: return@transaction false
        appointment.status = STATUS_CANCELLED
        appointment.slotKey = null
        true
    }

    /** The appointment when it belongs to [patientId], else null. */
    fun findAppointmentOf(patientId: Int, id: Int): Appointment? = transaction(database) {
        AppointmentEntity.find { ownedBy(patientId, id) }.withDetails().singleOrNull()?.toAppointment()
    }

    fun findAppointment(id: Int): Appointment? = transaction(database) {
        AppointmentEntity.findById(id)?.toAppointment()
    }

    private fun ownedBy(patientId: Int, id: Int): Op<Boolean> =
        (AppointmentsTable.id eq id) and (AppointmentsTable.patient eq patientId)

    /** Loads what [toAppointment] reads in a few queries, instead of one per appointment. */
    private fun SizedIterable<AppointmentEntity>.withDetails() = with(
        AppointmentEntity::patient,
        AppointmentEntity::professional,
        ProfessionalEntity::specialty,
        ProfessionalEntity::clinic,
        AppointmentEntity::preTriages,
    )

    private fun AppointmentEntity.toAppointment(): Appointment {
        val professional = professional
        return Appointment(
            id = id.value,
            specialtyName = professional.specialty.name,
            professionalName = professional.name,
            clinic = professional.clinic.toClinic(),
            date = date,
            time = time,
            status = when (status) {
                STATUS_CONFIRMED -> AppointmentStatus.Confirmed
                STATUS_CANCELLED -> AppointmentStatus.Cancelled
                else -> AppointmentStatus.Pending
            },
            patientName = patient.socialName.ifBlank { patient.fullName },
            contactPhone = Phone(patient.primaryPhone),
            reason = reason,
            notes = notes,
            preTriageId = preTriages.firstOrNull()?.id?.value,
        )
    }

    private fun ClinicEntity.toClinic(): Clinic = Clinic(
        id = id.value,
        name = name,
        city = city,
        timeZoneId = timeZone,
    )

    private fun AppointmentStatus.stored(): String = when (this) {
        AppointmentStatus.Pending -> STATUS_PENDING
        AppointmentStatus.Confirmed -> STATUS_CONFIRMED
        AppointmentStatus.Cancelled -> STATUS_CANCELLED
    }

    private companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_CANCELLED = "cancelled"
    }
}
