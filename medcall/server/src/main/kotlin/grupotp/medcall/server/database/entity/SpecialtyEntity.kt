package grupotp.medcall.server.database.entity

import grupotp.medcall.server.database.table.SpecialtiesTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class SpecialtyEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SpecialtyEntity>(SpecialtiesTable)

    var name by SpecialtiesTable.name
}
