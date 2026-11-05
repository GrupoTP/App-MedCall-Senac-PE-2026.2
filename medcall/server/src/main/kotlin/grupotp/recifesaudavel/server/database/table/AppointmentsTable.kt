package grupotp.recifesaudavel.server.database.table

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.time
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * [date] and [time] are wall-clock values at the professional's clinic, in [ClinicsTable.timeZone].
 *
 * [slotKey] is what stops two patients from taking the same slot (RN-010): it holds
 * [slotKeyOf] while the appointment is active and NULL once it is cancelled, so a cancelled
 * appointment stays in the history without blocking its slot. A unique index allows many
 * NULLs on both H2 and MySQL, which keeps this portable (no partial indexes).
 */
object AppointmentsTable : IntIdTable("appointments") {
    val patient = reference("patient_id", UsersTable).index()
    val professional = reference("professional_id", ProfessionalsTable)
    val date = date("appointment_date")
    val time = time("appointment_time")
    val status = varchar("status", 20)
    val reason = varchar("reason", 1000)
    val notes = varchar("notes", 1000)
    val createdAt = timestamp("created_at")
    val slotKey = varchar("slot_key", 60).nullable().uniqueIndex(SLOT_KEY_INDEX)

    fun slotKeyOf(professionalId: Int, date: LocalDate, time: LocalTime): String = "$professionalId|$date|$time"

    const val SLOT_KEY_INDEX = "appointments_slot_key"
}
