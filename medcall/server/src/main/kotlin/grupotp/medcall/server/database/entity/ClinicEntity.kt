package grupotp.recifesaudavel.server.database.entity

import grupotp.recifesaudavel.server.database.table.ClinicsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class ClinicEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ClinicEntity>(ClinicsTable)

    var name by ClinicsTable.name
    var city by ClinicsTable.city
    var timeZone by ClinicsTable.timeZone
}
