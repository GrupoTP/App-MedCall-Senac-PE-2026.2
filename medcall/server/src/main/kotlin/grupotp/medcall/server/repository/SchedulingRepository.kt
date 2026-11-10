package grupotp.medcall.server.repository

import grupotp.medcall.common.account.Phone
import grupotp.medcall.common.scheduling.Appointment
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.scheduling.Clinic
import grupotp.medcall.common.scheduling.Professional
import grupotp.medcall.common.scheduling.Specialty
import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.server.database.Actor
import grupotp.medcall.server.database.SchedulingCatalog
import grupotp.medcall.server.database.recordAppointmentEvent
import grupotp.medcall.server.database.entity.AppointmentEntity
import grupotp.medcall.server.database.entity.ClinicEntity
import grupotp.medcall.server.database.entity.ProfessionalEntity
import grupotp.medcall.server.database.entity.SpecialtyEntity
import grupotp.medcall.server.database.entity.UserEntity
import grupotp.medcall.server.database.table.AppointmentsTable
import grupotp.medcall.server.database.table.ClinicsTable
import grupotp.medcall.server.database.table.ProfessionalsTable
import grupotp.medcall.server.database.table.SpecialtiesTable
import grupotp.medcall.server.domain.NewAppointment
import grupotp.medcall.server.domain.Slot
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.notInList
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

    /** Clinics patients can book: those not switched off in the MedCall panel. */
    fun clinics(): List<Clinic> = transaction(database) {
        val closed = closedClinicIds()
        ClinicEntity.all().orderBy(ClinicsTable.name to SortOrder.ASC)
            .filter { it.id.value !in closed }
            .map { it.toClinic() }
    }

    fun findClinic(id: Int): Clinic? = transaction(database) {
        ClinicEntity.findById(id)?.toClinic()
    }

    /** Professionals patients can book: active, at an active clinic. */
    fun professionals(specialtyId: Int? = null): List<Professional> = transaction(database) {
        val hidden = hiddenProfessionalIds()
        val professionals = if (specialtyId == null) {
            ProfessionalEntity.find { ProfessionalsTable.id notInList hidden }
        } else {
            ProfessionalEntity.find { (ProfessionalsTable.specialty eq specialtyId) and (ProfessionalsTable.id notInList hidden) }
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

    /** Slots held by appointments: every status but declined and cancelled keeps its slot key. */
    fun takenSlots(professionalIds: List<Int>, dates: List<LocalDate>): Set<Slot> = transaction(database) {
        if (professionalIds.isEmpty() || dates.isEmpty()) {
            return@transaction emptySet()
        }
        AppointmentEntity.find {
            (AppointmentsTable.professional inList professionalIds) and
                (AppointmentsTable.date inList dates) and
                AppointmentsTable.slotKey.isNotNull()
        }.mapTo(mutableSetOf()) { Slot(it.professionalId.value, it.date, it.time) }
    }

    /**
     * Inserts the appointment and returns its id. Returns null when the slot was taken in the
     * meantime: the unique slot key rejects the second insert even under concurrent requests.
     */
    fun insert(appointment: NewAppointment): Int? = try {
        transaction(database) {
            val slot = appointment.slot
            val professional = ProfessionalEntity[slot.professionalId]
            val entity = AppointmentEntity.new {
                patient = UserEntity[appointment.patientId]
                this.professional = professional
                date = slot.date
                time = slot.time
                status = AppointmentStatus.Pending.stored()
                reason = appointment.reason
                notes = appointment.notes
                createdAt = appointment.createdAt
                slotKey = AppointmentsTable.slotKeyOf(slot.professionalId, slot.date, slot.time)
            }
            // The event references the row, which the DAO would otherwise only insert at commit.
            entity.flush()
            val id = entity.id.value
            recordAppointmentEvent(id, professional.clinicId.value, AppointmentEventType.Created, Actor.PATIENT, appointment.createdAt)
            id
        }
    } catch (error: ExposedSQLException) {
        // Only a taken slot is a conflict; anything else is a server error and must surface as one.
        if (!error.isUniqueViolation()) {
            throw error
        }
        null
    }

    fun appointmentsOf(patientId: Int): List<Appointment> = transaction(database) {
        val rows = AppointmentEntity.find { AppointmentsTable.patient eq patientId }
            .orderBy(AppointmentsTable.date to SortOrder.ASC, AppointmentsTable.time to SortOrder.ASC)
            .withDetails()
            .toList()
        val extras = appointmentExtras(rows.map { it.id.value })
        rows.map { it.toAppointment(extras[it.id.value]) }
    }

    /**
     * Marks the patient's own appointment as cancelled and clears its slot key, so the slot can
     * be booked again while the appointment stays in the history. Only changes it while its
     * status is still [expected]: the row is locked when read, so a confirmation that lands in
     * between is not overwritten. Returns false when nothing was changed.
     */
    fun cancel(patientId: Int, id: Int, expected: AppointmentStatus, at: Instant): Boolean = transaction(database) {
        val appointment = AppointmentEntity
            .find { ownedBy(patientId, id) and (AppointmentsTable.status eq expected.stored()) }
            .forUpdate()
            .singleOrNull()
            ?: return@transaction false
        appointment.status = AppointmentStatus.Cancelled.stored()
        appointment.slotKey = null
        recordAppointmentEvent(id, appointment.professional.clinicId.value, AppointmentEventType.Cancelled, Actor.PATIENT, at)
        true
    }

    /** The appointment when it belongs to [patientId], else null. */
    fun findAppointmentOf(patientId: Int, id: Int): Appointment? = transaction(database) {
        AppointmentEntity.find { ownedBy(patientId, id) }.withDetails().singleOrNull()
            ?.toAppointment(appointmentExtras(listOf(id))[id])
    }

    fun findAppointment(id: Int): Appointment? = transaction(database) {
        AppointmentEntity.findById(id)?.toAppointment(appointmentExtras(listOf(id))[id])
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

    private fun AppointmentEntity.toAppointment(extras: AppointmentExtras?): Appointment {
        val professional = professional
        return Appointment(
            id = id.value,
            specialtyName = professional.specialty.name,
            professionalName = professional.name,
            clinic = professional.clinic.toClinic(),
            date = date,
            time = time,
            status = storedStatus(status),
            patientName = patient.socialName.ifBlank { patient.fullName },
            contactPhone = Phone(patient.primaryPhone),
            reason = reason,
            notes = notes,
            preTriageId = preTriages.firstOrNull()?.id?.value,
            rescheduled = extras?.rescheduled ?: false,
            declineReason = extras?.declineReason.orEmpty(),
        )
    }

    private fun ClinicEntity.toClinic(): Clinic = Clinic(
        id = id.value,
        name = name,
        city = city,
        timeZoneId = timeZone,
    )
}
