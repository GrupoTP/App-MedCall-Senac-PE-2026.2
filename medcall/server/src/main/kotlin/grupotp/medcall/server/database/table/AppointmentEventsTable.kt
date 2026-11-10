package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.time
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Every status change of an appointment, insert only: rows are never updated or deleted. The KPIs
 * (docs/kpis.md) are computed from here, which is what makes "time until confirmation" and the
 * audit of who confirmed possible.
 *
 * [clinic] repeats the appointment's clinic so the dashboards filter without joining. No patient
 * data is copied here.
 *
 * @property type wire name of [grupotp.medcall.common.clinic.AppointmentEventType].
 * @property origin wire name of [grupotp.medcall.common.clinic.EventOrigin].
 * @property detail decline reason code, or other short text of the change.
 * @property previousDate with [previousTime], where a rescheduled appointment was before.
 */
object AppointmentEventsTable : IntIdTable("appointment_events") {
    val appointment = reference("appointment_id", AppointmentsTable, onDelete = ReferenceOption.CASCADE).index()
    val clinic = reference("clinic_id", ClinicsTable)
    val type = varchar("type", 30)
    val occurredAt = timestamp("occurred_at")
    val origin = varchar("origin", 20)
    val staffUser = optReference("staff_user_id", StaffUsersTable)
    val apiKey = optReference("api_key_id", ApiKeysTable)
    val detail = varchar("detail", 300).default("")
    val previousDate = date("previous_date").nullable()
    val previousTime = time("previous_time").nullable()

    init {
        index(false, clinic, occurredAt)
    }
}
