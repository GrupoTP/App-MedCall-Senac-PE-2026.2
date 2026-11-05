package grupotp.recifesaudavel.server.database.entity

import grupotp.recifesaudavel.server.database.table.AppointmentsTable
import grupotp.recifesaudavel.server.database.table.PreTriagesTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/** [professionalId] reads the column without loading the [professional]. */
class AppointmentEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AppointmentEntity>(AppointmentsTable)

    var patient by UserEntity referencedOn AppointmentsTable.patient
    var professional by ProfessionalEntity referencedOn AppointmentsTable.professional
    val professionalId by AppointmentsTable.professional
    var date by AppointmentsTable.date
    var time by AppointmentsTable.time
    var status by AppointmentsTable.status
    var reason by AppointmentsTable.reason
    var notes by AppointmentsTable.notes
    var createdAt by AppointmentsTable.createdAt
    var slotKey by AppointmentsTable.slotKey
    val preTriages by PreTriageEntity optionalReferrersOn PreTriagesTable.appointment
}
