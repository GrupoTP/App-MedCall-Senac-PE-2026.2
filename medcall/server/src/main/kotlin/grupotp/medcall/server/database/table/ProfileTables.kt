package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/*
 * Data the MedCall panel adds to clinics and professionals. They live in their own tables because
 * Exposed creates missing tables but never adds columns, and production already has data in
 * `clinics` and `professionals`. A clinic or professional without a row here is active.
 */

/** @property status wire name of [grupotp.medcall.common.kpi.ClinicStatus]. */
object ClinicProfilesTable : IntIdTable("clinic_profiles") {
    val clinic = reference("clinic_id", ClinicsTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val status = varchar("status", 20)
    val address = varchar("address", 300).default("")
    val phone = varchar("phone", 40).default("")
    val createdAt = timestamp("created_at")
}

object ProfessionalProfilesTable : IntIdTable("professional_profiles") {
    val professional = reference("professional_id", ProfessionalsTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val registration = varchar("registration", 40).default("")
    val active = bool("active")
}
