package grupotp.recifesaudavel.server.database

import org.jetbrains.exposed.v1.core.Table

/**
 * Pre-triages sent by patients. The answers are kept as one JSON document, so new questions
 * do not need a migration. Health data is sensitive (LGPD): only the owner reads it back.
 *
 * The link lives here, not in `appointments`, so existing databases need no new column.
 */
object PreTriagesTable : Table("pre_triages") {
    val id = varchar("id", 36)
    val patientId = varchar("patient_id", 36).references(UsersTable.id)
    val form = text("form")
    val createdAt = long("created_at")
    val appointmentId = varchar("appointment_id", 36).references(AppointmentsTable.id).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, patientId)
        index(false, appointmentId)
    }
}
