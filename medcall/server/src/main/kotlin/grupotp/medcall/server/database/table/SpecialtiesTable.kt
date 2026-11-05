package grupotp.recifesaudavel.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object SpecialtiesTable : IntIdTable("specialties") {
    val name = varchar("name", 100).uniqueIndex()
}
