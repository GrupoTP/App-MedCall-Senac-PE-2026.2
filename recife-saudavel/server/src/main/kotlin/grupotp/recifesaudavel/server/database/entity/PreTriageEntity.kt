package grupotp.recifesaudavel.server.database.entity

import grupotp.recifesaudavel.server.database.table.PreTriagesTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/** [form] is the JSON of the answers; [appointmentId] reads the column without loading the [appointment]. */
class PreTriageEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PreTriageEntity>(PreTriagesTable)

    var patient by UserEntity referencedOn PreTriagesTable.patient
    var form by PreTriagesTable.form
    var createdAt by PreTriagesTable.createdAt
    var appointment by AppointmentEntity optionalReferencedOn PreTriagesTable.appointment
    val appointmentId by PreTriagesTable.appointment
}
