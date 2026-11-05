package grupotp.recifesaudavel.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ProfessionalsTable : IntIdTable("professionals") {
    val name = varchar("name", 200)
    val specialty = reference("specialty_id", SpecialtiesTable).index()
    val clinic = reference("clinic_id", ClinicsTable)
}
