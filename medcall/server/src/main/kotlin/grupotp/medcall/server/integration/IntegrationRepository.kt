package grupotp.medcall.server.integration

import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.integration.ApiKey
import grupotp.medcall.common.integration.DeliveryStatus
import grupotp.medcall.common.integration.PublicAppointment
import grupotp.medcall.common.integration.Webhook
import grupotp.medcall.common.integration.WebhookDelivery
import grupotp.medcall.common.integration.WebhookEvent
import grupotp.medcall.server.database.table.ApiKeysTable
import grupotp.medcall.server.database.table.AppointmentEventsTable
import grupotp.medcall.server.database.table.AppointmentsTable
import grupotp.medcall.server.database.table.IdempotencyKeysTable
import grupotp.medcall.server.database.table.OutboxTable
import grupotp.medcall.server.database.table.ProfessionalsTable
import grupotp.medcall.server.database.table.SpecialtiesTable
import grupotp.medcall.server.database.table.StaffUsersTable
import grupotp.medcall.server.database.table.UsersTable
import grupotp.medcall.server.database.table.WebhookDeliveriesTable
import grupotp.medcall.server.database.table.WebhooksTable
import grupotp.medcall.server.database.wireName
import grupotp.medcall.server.database.wireValue
import grupotp.medcall.server.repository.isUniqueViolation
import grupotp.medcall.server.repository.storedStatus
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Instant

/** An outbox row waiting for the broker. */
data class OutboxMessage(
    val id: Int,
    val eventId: String,
    val clinicId: Int,
    val appointmentId: Int,
    val eventType: AppointmentEventType,
    val occurredAt: Instant,
)

/** A webhook with what only the server reads. */
data class StoredWebhook(
    val webhook: Webhook,
    val clinicId: Int,
    val secret: String,
    val previousSecret: String?,
    val previousSecretUntil: Instant?,
    val failingSince: Instant?,
)

data class StoredDelivery(
    val delivery: WebhookDelivery,
    val payload: String,
)

data class StoredIdempotency(val requestHash: String, val statusCode: Int, val body: String)

/** Tables of the clinic integration: API keys, webhooks, their deliveries and the outbox. */
class IntegrationRepository(
    private val database: Database,
) {
    // --- API keys ---

    fun insertApiKey(clinicId: Int, name: String, hash: String, lastFour: String, createdBy: Int, at: Instant): ApiKey = transaction(database) {
        val id = ApiKeysTable.insertAndGetId {
            it[clinic] = clinicId
            it[this.name] = name
            it[keyHash] = hash
            it[this.lastFour] = lastFour
            it[createdAt] = at
            it[this.createdBy] = createdBy
        }.value
        apiKeys(clinicId).single { it.id == id }
    }

    fun apiKeys(clinicId: Int): List<ApiKey> = transaction(database) {
        ApiKeysTable.join(StaffUsersTable, JoinType.LEFT, ApiKeysTable.createdBy, StaffUsersTable.id)
            .selectAll()
            .where { ApiKeysTable.clinic eq clinicId }
            .orderBy(ApiKeysTable.id to SortOrder.DESC)
            .map {
                ApiKey(
                    id = it[ApiKeysTable.id].value,
                    name = it[ApiKeysTable.name],
                    lastFour = it[ApiKeysTable.lastFour],
                    createdAt = it[ApiKeysTable.createdAt],
                    createdBy = it.getOrNull(StaffUsersTable.fullName) ?: "—",
                    lastUsedAt = it[ApiKeysTable.lastUsedAt],
                    revokedAt = it[ApiKeysTable.revokedAt],
                )
            }
    }

    fun revokeApiKey(clinicId: Int, id: Int, at: Instant): Boolean = transaction(database) {
        ApiKeysTable.update({ (ApiKeysTable.id eq id) and (ApiKeysTable.clinic eq clinicId) and ApiKeysTable.revokedAt.isNull() }) {
            it[revokedAt] = at
        } > 0
    }

    /** (key id, clinic id) of an active key with this hash. */
    fun findActiveKey(hash: String): Pair<Int, Int>? = transaction(database) {
        ApiKeysTable.selectAll()
            .where { (ApiKeysTable.keyHash eq hash) and ApiKeysTable.revokedAt.isNull() }
            .singleOrNull()
            ?.let { it[ApiKeysTable.id].value to it[ApiKeysTable.clinic].value }
    }

    fun touchApiKey(id: Int, at: Instant) {
        transaction(database) { ApiKeysTable.update({ ApiKeysTable.id eq id }) { it[lastUsedAt] = at } }
    }

    // --- webhooks ---

    fun insertWebhook(clinicId: Int, url: String, description: String, events: List<WebhookEvent>, active: Boolean, secret: String, at: Instant): Int =
        transaction(database) {
            WebhooksTable.insertAndGetId {
                it[clinic] = clinicId
                it[this.url] = url
                it[this.description] = description
                it[this.events] = events.joinToString(",") { event -> event.wireName() }
                it[this.secret] = secret
                it[this.active] = active
                it[createdAt] = at
            }.value
        }

    fun updateWebhook(clinicId: Int, id: Int, url: String, description: String, events: List<WebhookEvent>, active: Boolean): Boolean =
        transaction(database) {
            WebhooksTable.update({ (WebhooksTable.id eq id) and (WebhooksTable.clinic eq clinicId) }) {
                it[this.url] = url
                it[this.description] = description
                it[this.events] = events.joinToString(",") { event -> event.wireName() }
                it[this.active] = active
                if (active) {
                    it[disabledReason] = ""
                    it[failingSince] = null
                }
            } > 0
        }

    fun rotateSecret(clinicId: Int, id: Int, secret: String, keepOldUntil: Instant, at: Instant): Boolean = transaction(database) {
        val current = WebhooksTable.selectAll().where { (WebhooksTable.id eq id) and (WebhooksTable.clinic eq clinicId) }.singleOrNull()
            ?: return@transaction false
        WebhooksTable.update({ WebhooksTable.id eq id }) {
            it[previousSecret] = current[WebhooksTable.secret]
            it[previousSecretUntil] = keepOldUntil
            it[this.secret] = secret
            it[secretRotatedAt] = at
        }
        true
    }

    fun webhooks(clinicId: Int): List<StoredWebhook> = transaction(database) {
        WebhooksTable.selectAll().where { WebhooksTable.clinic eq clinicId }.orderBy(WebhooksTable.id to SortOrder.ASC).map { it.toWebhook() }
    }

    fun webhook(id: Int): StoredWebhook? = transaction(database) {
        WebhooksTable.selectAll().where { WebhooksTable.id eq id }.singleOrNull()?.toWebhook()
    }

    fun markFailing(id: Int, since: Instant?) {
        transaction(database) { WebhooksTable.update({ WebhooksTable.id eq id }) { it[failingSince] = since } }
    }

    fun disable(id: Int, reason: String) {
        transaction(database) {
            WebhooksTable.update({ WebhooksTable.id eq id }) {
                it[active] = false
                it[disabledReason] = reason
            }
        }
    }

    private fun ResultRow.toWebhook() = StoredWebhook(
        webhook = Webhook(
            id = this[WebhooksTable.id].value,
            url = this[WebhooksTable.url],
            description = this[WebhooksTable.description],
            events = this[WebhooksTable.events].split(",").filter { it.isNotBlank() }.map { wireValue<WebhookEvent>(it) },
            active = this[WebhooksTable.active],
            createdAt = this[WebhooksTable.createdAt],
            disabledReason = this[WebhooksTable.disabledReason],
            secretRotatedAt = this[WebhooksTable.secretRotatedAt],
        ),
        clinicId = this[WebhooksTable.clinic].value,
        secret = this[WebhooksTable.secret],
        previousSecret = this[WebhooksTable.previousSecret],
        previousSecretUntil = this[WebhooksTable.previousSecretUntil],
        failingSince = this[WebhooksTable.failingSince],
    )

    // --- deliveries ---

    /** Null when this webhook already has a delivery of this event: delivery is at least once, never twice by us. */
    fun insertDelivery(webhookId: Int, eventId: String, eventType: String, payload: String, at: Instant): Int? = try {
        transaction(database) {
            WebhookDeliveriesTable.insertAndGetId {
                it[webhook] = webhookId
                it[this.eventId] = eventId
                it[this.eventType] = eventType
                it[this.payload] = payload
                it[status] = DeliveryStatus.Pending.wireName()
                it[attempts] = 0
                it[createdAt] = at
                it[nextAttemptAt] = at
            }.value
        }
    } catch (error: ExposedSQLException) {
        if (!error.isUniqueViolation()) throw error
        null
    }

    fun delivery(id: Int): StoredDelivery? = transaction(database) {
        WebhookDeliveriesTable.selectAll().where { WebhookDeliveriesTable.id eq id }.singleOrNull()?.toDelivery()
    }

    /** Deliveries of the clinic's webhooks, newest first. */
    fun deliveries(clinicId: Int, webhookId: Int?, limit: Int): List<WebhookDelivery> = transaction(database) {
        val ids = WebhooksTable.selectAll().where { WebhooksTable.clinic eq clinicId }.map { it[WebhooksTable.id].value }
            .filter { webhookId == null || it == webhookId }
        if (ids.isEmpty()) return@transaction emptyList()
        WebhookDeliveriesTable.selectAll().where { WebhookDeliveriesTable.webhook inList ids }
            .orderBy(WebhookDeliveriesTable.id to SortOrder.DESC)
            .limit(limit)
            .map { it.toDelivery().delivery }
    }

    /** Deliveries whose next attempt is due. */
    fun dueDeliveries(now: Instant, limit: Int): List<StoredDelivery> = transaction(database) {
        WebhookDeliveriesTable.selectAll()
            .where {
                (WebhookDeliveriesTable.status inList listOf(DeliveryStatus.Pending.wireName(), DeliveryStatus.Retrying.wireName())) and
                    (WebhookDeliveriesTable.nextAttemptAt lessEq now)
            }
            .orderBy(WebhookDeliveriesTable.nextAttemptAt to SortOrder.ASC)
            .limit(limit)
            .map { it.toDelivery() }
    }

    fun recordAttempt(id: Int, status: DeliveryStatus, attempts: Int, code: Int?, durationMs: Long, error: String, next: Instant?) {
        transaction(database) {
            WebhookDeliveriesTable.update({ WebhookDeliveriesTable.id eq id }) {
                it[this.status] = status.wireName()
                it[this.attempts] = attempts
                it[lastStatusCode] = code
                it[lastDurationMs] = durationMs
                it[lastError] = error.take(300)
                it[nextAttemptAt] = next
            }
        }
    }

    /** Puts a dead delivery back in line. */
    fun requeue(clinicId: Int, id: Int, at: Instant): Boolean = transaction(database) {
        val stored = WebhookDeliveriesTable.innerJoin(WebhooksTable).selectAll()
            .where { (WebhookDeliveriesTable.id eq id) and (WebhooksTable.clinic eq clinicId) }
            .singleOrNull() ?: return@transaction false
        if (stored[WebhookDeliveriesTable.status] != DeliveryStatus.Dead.wireName()) return@transaction false
        WebhookDeliveriesTable.update({ WebhookDeliveriesTable.id eq id }) {
            it[status] = DeliveryStatus.Pending.wireName()
            it[attempts] = 0
            it[nextAttemptAt] = at
        }
        true
    }

    private fun ResultRow.toDelivery() = StoredDelivery(
        delivery = WebhookDelivery(
            id = this[WebhookDeliveriesTable.id].value,
            webhookId = this[WebhookDeliveriesTable.webhook].value,
            eventId = this[WebhookDeliveriesTable.eventId],
            eventType = this[WebhookDeliveriesTable.eventType],
            status = wireValue(this[WebhookDeliveriesTable.status]),
            attempts = this[WebhookDeliveriesTable.attempts],
            lastStatusCode = this[WebhookDeliveriesTable.lastStatusCode],
            lastDurationMs = this[WebhookDeliveriesTable.lastDurationMs],
            lastError = this[WebhookDeliveriesTable.lastError],
            createdAt = this[WebhookDeliveriesTable.createdAt],
            nextAttemptAt = this[WebhookDeliveriesTable.nextAttemptAt],
        ),
        payload = this[WebhookDeliveriesTable.payload],
    )

    // --- outbox ---

    fun unpublished(limit: Int): List<OutboxMessage> = transaction(database) {
        OutboxTable.selectAll().where { OutboxTable.publishedAt.isNull() }
            .orderBy(OutboxTable.id to SortOrder.ASC)
            .limit(limit)
            .map {
                OutboxMessage(
                    id = it[OutboxTable.id].value,
                    eventId = it[OutboxTable.eventId],
                    clinicId = it[OutboxTable.clinic].value,
                    appointmentId = it[OutboxTable.appointment].value,
                    eventType = wireValue(it[OutboxTable.eventType]),
                    occurredAt = it[OutboxTable.occurredAt],
                )
            }
    }

    fun markPublished(id: Int, at: Instant) {
        transaction(database) { OutboxTable.update({ OutboxTable.id eq id }) { it[publishedAt] = at } }
    }

    // --- appointments as the clinic's system sees them ---

    fun publicAppointments(clinicId: Int, ids: List<Int>? = null, from: kotlinx.datetime.LocalDate? = null, to: kotlinx.datetime.LocalDate? = null, status: String? = null): List<PublicAppointment> =
        transaction(database) {
            val query = AppointmentsTable.innerJoin(ProfessionalsTable).innerJoin(SpecialtiesTable).innerJoin(UsersTable).selectAll()
            query.where {
                var condition = ProfessionalsTable.clinic eq clinicId
                if (ids != null) condition = condition and (AppointmentsTable.id inList ids)
                if (from != null && to != null) {
                    condition = condition and AppointmentsTable.date.between(from, to)
                }
                if (status != null) condition = condition and (AppointmentsTable.status eq status)
                condition
            }
            val rows = query.orderBy(AppointmentsTable.date to SortOrder.ASC, AppointmentsTable.time to SortOrder.ASC).limit(MAX_LIST).toList()
            val appointmentIds = rows.map { it[AppointmentsTable.id].value }
            val lastEvents = mutableMapOf<Int, AppointmentEventType>()
            appointmentIds.chunked(1000).forEach { chunk ->
                AppointmentEventsTable.selectAll().where { AppointmentEventsTable.appointment inList chunk }
                    .orderBy(AppointmentEventsTable.id to SortOrder.ASC)
                    .forEach { lastEvents[it[AppointmentEventsTable.appointment].value] = wireValue(it[AppointmentEventsTable.type]) }
            }
            rows.map {
                val id = it[AppointmentsTable.id].value
                PublicAppointment(
                    id = id,
                    status = storedStatus(it[AppointmentsTable.status]),
                    date = it[AppointmentsTable.date],
                    time = it[AppointmentsTable.time],
                    professionalId = it[AppointmentsTable.professional].value,
                    professionalName = it[ProfessionalsTable.name],
                    specialtyName = it[SpecialtiesTable.name],
                    patientName = it[UsersTable.socialName].ifBlank { it[UsersTable.fullName] },
                    patientPhone = it[UsersTable.primaryPhone],
                    reason = it[AppointmentsTable.reason],
                    createdAt = it[AppointmentsTable.createdAt],
                    lastEvent = lastEvents[id],
                )
            }
        }

    // --- idempotency ---

    fun idempotency(apiKeyId: Int, key: String): StoredIdempotency? = transaction(database) {
        IdempotencyKeysTable.selectAll()
            .where { (IdempotencyKeysTable.apiKey eq apiKeyId) and (IdempotencyKeysTable.key eq key) }
            .singleOrNull()
            ?.let { StoredIdempotency(it[IdempotencyKeysTable.requestHash], it[IdempotencyKeysTable.statusCode], it[IdempotencyKeysTable.body]) }
    }

    fun storeIdempotency(apiKeyId: Int, key: String, requestHash: String, statusCode: Int, body: String, at: Instant) {
        try {
            transaction(database) {
                IdempotencyKeysTable.insert {
                    it[apiKey] = apiKeyId
                    it[this.key] = key
                    it[this.requestHash] = requestHash
                    it[this.statusCode] = statusCode
                    it[this.body] = body
                    it[createdAt] = at
                }
            }
        } catch (error: ExposedSQLException) {
            // A concurrent retry with the same key stored it first; its answer stands.
            if (!error.isUniqueViolation()) throw error
        }
    }

    private companion object {
        const val MAX_LIST = 500
    }
}
