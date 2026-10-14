package grupotp.recifesaudavel.server.database

import org.jetbrains.exposed.v1.core.Table

object SpecialtiesTable : Table("specialties") {
    val id = varchar("id", 36)
    val name = varchar("name", 100)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(name)
    }
}

object ProfessionalsTable : Table("professionals") {
    val id = varchar("id", 36)
    val name = varchar("name", 200)
    val specialtyId = varchar("specialty_id", 36).references(SpecialtiesTable.id)

    override val primaryKey = PrimaryKey(id)
}

/**
 * Dates are ISO `yyyy-MM-dd` and times `HH:mm`, so text order is time order on any database.
 * The unique index is what stops two patients from taking the same slot (RN-010).
 */
object AppointmentsTable : Table("appointments") {
    val id = varchar("id", 36)
    val patientId = varchar("patient_id", 36).references(UsersTable.id)
    val professionalId = varchar("professional_id", 36).references(ProfessionalsTable.id)
    val appointmentDate = varchar("appointment_date", 10)
    val appointmentTime = varchar("appointment_time", 5)
    val status = varchar("status", 20)
    val reason = varchar("reason", 1000)
    val notes = varchar("notes", 1000)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("appointments_slot", professionalId, appointmentDate, appointmentTime)
        index(false, patientId)
    }
}
