package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** Who did what in the portals. Holds names of things, never patient data. */
object AuditLogTable : IntIdTable("audit_log") {
    val occurredAt = timestamp("occurred_at")
    val staffUser = optReference("staff_user_id", StaffUsersTable)
    val apiKey = optReference("api_key_id", ApiKeysTable)
    val clinic = optReference("clinic_id", ClinicsTable).index()
    val action = varchar("action", 60)
    val target = varchar("target", 200)
}

