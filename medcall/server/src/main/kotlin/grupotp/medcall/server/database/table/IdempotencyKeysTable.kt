package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** Answers already given to a public API call, by its `Idempotency-Key`. */
object IdempotencyKeysTable : IntIdTable("idempotency_keys") {
    val apiKey = reference("api_key_id", ApiKeysTable, onDelete = ReferenceOption.CASCADE)
    val key = varchar("idempotency_key", 100)
    val requestHash = varchar("request_hash", 64)
    val statusCode = integer("status_code")
    val body = text("body")
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(apiKey, key)
    }
}

