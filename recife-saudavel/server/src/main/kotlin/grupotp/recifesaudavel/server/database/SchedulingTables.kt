package grupotp.recifesaudavel.server.database

import org.jetbrains.exposed.v1.core.Table

/** Where professionals attend. [timeZone] is an IANA id: appointment times are local to it. */
object ClinicsTable : Table("clinics") {
    val id = varchar("id", 36)
    val name = varchar("name", 200)
    val city = varchar("city", 100)
    val timeZone = varchar("time_zone", 64)

    override val primaryKey = PrimaryKey(id)
}

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
    val clinicId = varchar("clinic_id", 36).references(ClinicsTable.id)

    override val primaryKey = PrimaryKey(id)
}

/**
 * Dates are ISO `yyyy-MM-dd` and times `HH:mm`, so text order is time order on any database.
 * Both are wall-clock values at the professional's clinic, in [ClinicsTable.timeZone].
 *
 * [slotKey] is what stops two patients from taking the same slot (RN-010): it holds
 * [slotKeyOf] while the appointment is active and NULL once it is cancelled, so a cancelled
 * appointment stays in the history without blocking its slot. A unique index allows many
 * NULLs on both H2 and MySQL, which keeps this portable (no partial indexes).
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
    val slotKey = varchar("slot_key", 60).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(SLOT_KEY_INDEX, slotKey)
        index(false, patientId)
    }

    fun slotKeyOf(professionalId: String, date: String, time: String): String = "$professionalId|$date|$time"

    const val SLOT_KEY_INDEX = "appointments_slot_key"
}
