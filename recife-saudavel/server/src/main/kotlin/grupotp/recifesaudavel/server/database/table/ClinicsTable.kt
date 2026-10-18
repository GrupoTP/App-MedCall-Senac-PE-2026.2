package grupotp.recifesaudavel.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

/** Where professionals attend. [timeZone] is an IANA id: appointment times are local to it. */
object ClinicsTable : IntIdTable("clinics") {
    val name = varchar("name", 200)
    val city = varchar("city", 100)
    val timeZone = varchar("time_zone", 64)
}
