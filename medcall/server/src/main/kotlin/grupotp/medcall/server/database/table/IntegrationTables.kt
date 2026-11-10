package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
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

/**
 * Where a clinic wants its events posted. The signing secret has to be read back to sign, so it is
 * kept as is; it only leaves the server once, when created or rotated.
 *
 * @property events wire names of [grupotp.medcall.common.integration.WebhookEvent], comma separated.
 * @property previousSecret still accepted until [previousSecretUntil] after a rotation.
 */
object WebhooksTable : IntIdTable("webhooks") {
    val clinic = reference("clinic_id", ClinicsTable).index()
    val url = varchar("url", 500)
    val description = varchar("description", 200).default("")
    val events = varchar("events", 300)
    val secret = varchar("secret", 100)
    val previousSecret = varchar("previous_secret", 100).nullable()
    val previousSecretUntil = timestamp("previous_secret_until").nullable()
    val secretRotatedAt = timestamp("secret_rotated_at").nullable()
    val active = bool("active")
    val disabledReason = varchar("disabled_reason", 200).default("")
    val failingSince = timestamp("failing_since").nullable()
    val createdAt = timestamp("created_at")
}

/**
 * Transactional outbox: written in the same transaction as the appointment event, then published
 * to the broker by [grupotp.medcall.server.integration.OutboxPublisher]. A broker outage never
 * loses an event, and an aborted transaction never publishes one.
 */
object OutboxTable : IntIdTable("outbox") {
    val eventId = varchar("event_id", 36).uniqueIndex()
    val clinic = reference("clinic_id", ClinicsTable)
    val appointment = reference("appointment_id", AppointmentsTable, onDelete = ReferenceOption.CASCADE)
    val eventType = varchar("event_type", 40)
    val occurredAt = timestamp("occurred_at")
    val publishedAt = timestamp("published_at").nullable().index()
}

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
