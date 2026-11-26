package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Keys a clinic's own system uses on the public API. Only the SHA-256 of the key is stored.
 */
object ApiKeysTable : IntIdTable("api_keys") {
    val clinic = reference("clinic_id", ClinicsTable).index()
    val name = varchar("name", 100)
    val keyHash = varchar("key_hash", 64).uniqueIndex()
    val lastFour = varchar("last_four", 4)
    val createdAt = timestamp("created_at")
    val createdBy = optReference("created_by", StaffUsersTable)
    val lastUsedAt = timestamp("last_used_at").nullable()
    val revokedAt = timestamp("revoked_at").nullable()
}

