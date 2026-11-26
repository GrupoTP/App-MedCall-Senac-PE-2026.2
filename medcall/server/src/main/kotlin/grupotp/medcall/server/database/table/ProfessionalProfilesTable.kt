package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

/*
 * Data the MedCall panel adds to clinics and professionals. They live in their own tables because
 * Exposed creates missing tables but never adds columns, and production already has data in
 * `clinics` and `professionals`. A clinic or professional without a row here is active.
 */

object ProfessionalProfilesTable : IntIdTable("professional_profiles") {
    val professional = reference("professional_id", ProfessionalsTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val registration = varchar("registration", 40).default("")
    val active = bool("active")
}

