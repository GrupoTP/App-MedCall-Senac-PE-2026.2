package grupotp.recifesaudavel.server.database.entity

import grupotp.recifesaudavel.server.database.table.ProfessionalsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/** [specialtyId] and [clinicId] read the columns without loading the referenced rows. */
class ProfessionalEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ProfessionalEntity>(ProfessionalsTable)

    var name by ProfessionalsTable.name
    var specialty by SpecialtyEntity referencedOn ProfessionalsTable.specialty
    val specialtyId by ProfessionalsTable.specialty
    var clinic by ClinicEntity referencedOn ProfessionalsTable.clinic
    val clinicId by ProfessionalsTable.clinic
}
