package grupotp.medcall.server.repository

import grupotp.medcall.common.admin.AuditEntry
import grupotp.medcall.server.database.table.ApiKeysTable
import grupotp.medcall.server.database.table.AuditLogTable
import grupotp.medcall.server.database.table.StaffUsersTable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

/** Who did what in the portals. */
class AuditRepository(
    private val database: Database,
) {
    fun record(at: Instant, action: String, target: String, clinicId: Int?, staffUserId: Int? = null, apiKeyId: Int? = null) {
        transaction(database) {
            AuditLogTable.insert {
                it[occurredAt] = at
                it[this.action] = action
                it[this.target] = target.take(200)
                it[clinic] = clinicId
                it[staffUser] = staffUserId
                it[apiKey] = apiKeyId
            }
        }
    }

    /** Newest first. */
    fun list(clinicId: Int?, limit: Int): List<AuditEntry> = transaction(database) {
        AuditLogTable
            .join(StaffUsersTable, JoinType.LEFT, AuditLogTable.staffUser, StaffUsersTable.id)
            .join(ApiKeysTable, JoinType.LEFT, AuditLogTable.apiKey, ApiKeysTable.id)
            .selectAll()
            .apply { if (clinicId != null) where { AuditLogTable.clinic eq clinicId } }
            .orderBy(AuditLogTable.id to SortOrder.DESC)
            .limit(limit)
            .map {
                AuditEntry(
                    occurredAt = it[AuditLogTable.occurredAt],
                    actorName = it.getOrNull(StaffUsersTable.fullName)
                        ?: it.getOrNull(ApiKeysTable.name)?.let { name -> "Chave de API \"$name\"" }
                        ?: "Sistema",
                    action = it[AuditLogTable.action],
                    target = it[AuditLogTable.target],
                    clinicId = it[AuditLogTable.clinic]?.value,
                )
            }
    }
}

