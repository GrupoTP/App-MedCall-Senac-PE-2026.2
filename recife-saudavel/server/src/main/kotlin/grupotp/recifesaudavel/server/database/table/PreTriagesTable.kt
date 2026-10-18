package grupotp.recifesaudavel.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Pre-triages sent by patients. The answers are kept as one JSON document, so new questions
 * do not need a migration. Health data is sensitive (LGPD): only the owner reads it back.
 */
object PreTriagesTable : IntIdTable("pre_triages") {
    val patient = reference("patient_id", UsersTable).index()
    val form = text("form")
    val createdAt = timestamp("created_at")
    val appointment = optReference("appointment_id", AppointmentsTable).index()
}
