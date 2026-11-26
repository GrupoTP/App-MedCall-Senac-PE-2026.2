package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** One webhook post of one event, with its retries. */
object WebhookDeliveriesTable : IntIdTable("webhook_deliveries") {
    val webhook = reference("webhook_id", WebhooksTable, onDelete = ReferenceOption.CASCADE).index()
    val eventId = varchar("event_id", 36)
    val eventType = varchar("event_type", 40)
    val payload = text("payload")
    val status = varchar("status", 20).index()
    val attempts = integer("attempts")
    val lastStatusCode = integer("last_status_code").nullable()
    val lastDurationMs = long("last_duration_ms").nullable()
    val lastError = varchar("last_error", 300).default("")
    val createdAt = timestamp("created_at")
    val nextAttemptAt = timestamp("next_attempt_at").nullable()

    init {
        uniqueIndex(webhook, eventId)
    }
}

