package grupotp.medcall.server.integration

import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.integration.ApiKey
import grupotp.medcall.common.integration.ApiKeyWithSecret
import grupotp.medcall.common.integration.DeliveryStatus
import grupotp.medcall.common.integration.PublicAppointment
import grupotp.medcall.common.integration.SaveWebhookRequest
import grupotp.medcall.common.integration.Webhook
import grupotp.medcall.common.integration.WebhookDelivery
import grupotp.medcall.common.integration.WebhookEvent
import grupotp.medcall.common.integration.WebhookPayload
import grupotp.medcall.common.integration.WebhookTestResult
import grupotp.medcall.common.integration.WebhookWithSecret
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.common.security.SecretString
import grupotp.medcall.server.database.wireName
import grupotp.medcall.server.domain.ServiceError
import grupotp.medcall.server.repository.AuditRepository
import grupotp.medcall.server.security.StaffPrincipal
import grupotp.medcall.server.service.StaffResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/** A clinic system authenticated by an API key. */
data class ApiPrincipal(val apiKeyId: Int, val clinicId: Int)

/**
 * The clinic integration (SPM-97 to SPM-100): API keys, webhooks, and the pipeline
 * outbox → [EventBus] → deliveries → HTTP with retries and dead letters.
 */
class IntegrationService(
    private val repository: IntegrationRepository,
    private val sender: WebhookSender,
    private val bus: EventBus,
    private val audit: AuditRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(IntegrationService::class.java)
    private val json = Json { encodeDefaults = true }
    private val random = SecureRandom()
    private val lastTouch = ConcurrentHashMap<Int, Instant>()
    private val windows = ConcurrentHashMap<Int, Pair<Long, Int>>()

    val busName: String get() = bus.name

    // --- API keys (SPM-100) ---

    fun apiKeys(clinicId: Int): List<ApiKey> = repository.apiKeys(clinicId)

    fun createApiKey(actor: StaffPrincipal, clinicId: Int, name: String): StaffResult<ApiKeyWithSecret> {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 100) return StaffResult.Rejected(ServiceError(ErrorCode.NAME_REQUIRED))
        if (repository.apiKeys(clinicId).count { it.revokedAt == null } >= MAX_KEYS) {
            return StaffResult.Rejected(ServiceError(ErrorCode.API_KEY_LIMIT, MAX_KEYS))
        }
        val key = "mc_live_" + token(32)
        val created = repository.insertApiKey(clinicId, trimmed, sha256(key), key.takeLast(4), actor.staffId, now())
        audit.record(now(), "api_key.criada", trimmed, clinicId, actor.staffId)
        return StaffResult.Done(ApiKeyWithSecret(created, SecretString(key)))
    }

    fun revokeApiKey(actor: StaffPrincipal, clinicId: Int, id: Int): StaffResult<ApiKey> {
        if (!repository.revokeApiKey(clinicId, id, now())) return StaffResult.NotFound
        val key = repository.apiKeys(clinicId).single { it.id == id }
        audit.record(now(), "api_key.revogada", key.name, clinicId, actor.staffId)
        return StaffResult.Done(key)
    }

    /** The clinic of an active key, or null. The last use is written at most every few minutes. */
    fun authenticate(key: String): ApiPrincipal? {
        if (!key.startsWith("mc_live_")) return null
        val (id, clinicId) = repository.findActiveKey(sha256(key)) ?: return null
        val now = now()
        if (lastTouch[id]?.let { now - it < 5.minutes } != true) {
            lastTouch[id] = now
            repository.touchApiKey(id, now)
        }
        return ApiPrincipal(id, clinicId)
    }

    /** True while [apiKeyId] stays within [RATE_PER_MINUTE] calls in the current minute. */
    fun withinRateLimit(apiKeyId: Int): Boolean {
        val minute = now().epochSeconds / 60
        val updated = windows.compute(apiKeyId) { _, current ->
            if (current == null || current.first != minute) minute to 1 else minute to current.second + 1
        }!!
        return updated.second <= RATE_PER_MINUTE
    }

    // --- webhooks (SPM-97) ---

    fun webhooks(clinicId: Int): List<Webhook> = repository.webhooks(clinicId).map { it.webhook }

    fun createWebhook(actor: StaffPrincipal, clinicId: Int, request: SaveWebhookRequest): StaffResult<WebhookWithSecret> {
        validate(request)?.let { return StaffResult.Rejected(it) }
        val secret = "whsec_" + token(32)
        val id = repository.insertWebhook(clinicId, request.url.trim(), request.description.trim(), request.events.distinct(), request.active, secret, now())
        audit.record(now(), "webhook.criado", request.url.trim(), clinicId, actor.staffId)
        return StaffResult.Done(WebhookWithSecret(repository.webhook(id)!!.webhook, SecretString(secret)))
    }

    fun updateWebhook(actor: StaffPrincipal, clinicId: Int, id: Int, request: SaveWebhookRequest): StaffResult<Webhook> {
        validate(request)?.let { return StaffResult.Rejected(it) }
        if (!repository.updateWebhook(clinicId, id, request.url.trim(), request.description.trim(), request.events.distinct(), request.active)) {
            return StaffResult.NotFound
        }
        audit.record(now(), "webhook.alterado", request.url.trim(), clinicId, actor.staffId)
        return StaffResult.Done(repository.webhook(id)!!.webhook)
    }

    /** New secret; the old one keeps signing alongside for [OLD_SECRET_GRACE]. */
    fun rotateSecret(actor: StaffPrincipal, clinicId: Int, id: Int): StaffResult<WebhookWithSecret> {
        val secret = "whsec_" + token(32)
        if (!repository.rotateSecret(clinicId, id, secret, now() + OLD_SECRET_GRACE, now())) return StaffResult.NotFound
        val stored = repository.webhook(id)!!
        audit.record(now(), "webhook.segredo_trocado", stored.webhook.url, clinicId, actor.staffId)
        return StaffResult.Done(WebhookWithSecret(stored.webhook, SecretString(secret)))
    }

    /** Posts a sample event right away and reports the answer ("Enviar teste"). */
    suspend fun test(clinicId: Int, id: Int): StaffResult<WebhookTestResult> {
        val stored = repository.webhook(id)?.takeIf { it.clinicId == clinicId } ?: return StaffResult.NotFound
        val eventId = "test-" + token(12)
        val payload = WebhookPayload(
            eventId = eventId,
            type = "medcall.test",
            occurredAt = now(),
            clinicId = clinicId,
            appointment = PublicAppointment(
                id = 0, status = AppointmentStatus.Pending, date = LocalDate(2026, 12, 1), time = LocalTime(9, 0),
                professionalId = 0, professionalName = "Profissional de exemplo", specialtyName = "Clínica Geral",
                patientName = "Paciente de exemplo", patientPhone = "81999999999", reason = "Evento de teste",
                createdAt = now(), lastEvent = AppointmentEventType.Created,
            ),
        )
        val result = withContext(Dispatchers.IO) {
            sender.send(stored.webhook.url, json.encodeToString(payload), "medcall.test", eventId, secretsOf(stored), now().epochSeconds)
        }
        return StaffResult.Done(WebhookTestResult(result.statusCode, result.durationMs, result.error))
    }

    fun deliveries(clinicId: Int, webhookId: Int?): List<WebhookDelivery> = repository.deliveries(clinicId, webhookId, limit = 100)

    fun redeliver(clinicId: Int, id: Int): StaffResult<WebhookDelivery> {
        if (!repository.requeue(clinicId, id, now())) return StaffResult.NotFound
        return StaffResult.Done(repository.delivery(id)!!.delivery)
    }

    private fun validate(request: SaveWebhookRequest): ServiceError? = when {
        request.events.isEmpty() -> ServiceError(ErrorCode.WEBHOOK_EVENTS_REQUIRED)
        request.url.length > 500 || sender.urlProblem(request.url) != null -> ServiceError(ErrorCode.WEBHOOK_URL_INVALID)
        request.description.length > 200 -> ServiceError(ErrorCode.TEXT_TOO_LONG, 200)
        else -> null
    }

    // --- pipeline (SPM-98) ---

    /** Starts the outbox publisher, the bus consumer and the retry loop. */
    fun start(scope: CoroutineScope) {
        bus.subscribe(scope) { event -> withContext(Dispatchers.IO) { dispatch(event) } }
        scope.launch(Dispatchers.IO) {
            while (true) {
                runCatching { publishOutbox() }.onFailure { log.warn("Outbox: {}", it.message) }
                delay(OUTBOX_INTERVAL)
            }
        }
        scope.launch(Dispatchers.IO) {
            while (true) {
                runCatching { attemptDue() }.onFailure { log.warn("Webhook retries: {}", it.message) }
                delay(RETRY_INTERVAL)
            }
        }
        log.info("Webhook pipeline started on {}.", bus.name)
    }

    /** Sends unpublished outbox rows to the bus; a row stays until the broker confirms it. */
    fun publishOutbox() {
        for (message in repository.unpublished(OUTBOX_BATCH)) {
            bus.publish(
                BusEvent(message.eventId, message.clinicId, message.appointmentId, message.eventType.wireName(), message.occurredAt.toEpochMilliseconds()),
            )
            repository.markPublished(message.id, now())
        }
    }

    /** One bus event: a delivery per subscribed webhook of the clinic, first attempt at once. */
    fun dispatch(event: BusEvent) {
        val type = AppointmentEventType.entries.firstOrNull { it.wireName() == event.type } ?: return
        val hooks = repository.webhooks(event.clinicId).filter { it.webhook.active }
        if (hooks.isEmpty()) return
        val appointment = repository.publicAppointments(event.clinicId, ids = listOf(event.appointmentId)).singleOrNull() ?: return
        for (hook in hooks) {
            val webhookEvent = webhookEventFor(type, hook.webhook.events) ?: continue
            val payload = WebhookPayload(
                eventId = event.eventId,
                type = webhookEvent.wireName,
                occurredAt = Instant.fromEpochMilliseconds(event.occurredAtEpochMs),
                clinicId = event.clinicId,
                appointment = appointment,
            )
            val id = repository.insertDelivery(hook.webhook.id, event.eventId, webhookEvent.wireName, json.encodeToString(payload), now()) ?: continue
            attempt(repository.delivery(id)!!)
        }
    }

    fun attemptDue() {
        repository.dueDeliveries(now(), RETRY_BATCH).forEach(::attempt)
    }

    private fun attempt(stored: StoredDelivery) {
        val delivery = stored.delivery
        val hook = repository.webhook(delivery.webhookId) ?: return
        if (!hook.webhook.active) return
        val result = sender.send(hook.webhook.url, stored.payload, delivery.eventType, delivery.eventId, secretsOf(hook), now().epochSeconds)
        val attempts = delivery.attempts + 1
        val permanent = result.statusCode != null && result.statusCode in 400..499 && result.statusCode != 408 && result.statusCode != 429
        when {
            result.delivered -> {
                repository.recordAttempt(delivery.id, DeliveryStatus.Delivered, attempts, result.statusCode, result.durationMs, "", null)
                if (hook.failingSince != null) repository.markFailing(hook.webhook.id, null)
            }
            permanent || attempts >= MAX_ATTEMPTS -> {
                repository.recordAttempt(delivery.id, DeliveryStatus.Dead, attempts, result.statusCode, result.durationMs, result.error, null)
                bus.deadLetter(
                    BusEvent(delivery.eventId, hook.clinicId, 0, delivery.eventType, delivery.createdAt.toEpochMilliseconds()),
                    result.error.ifBlank { "HTTP ${result.statusCode}" },
                )
                noteFailure(hook)
            }
            else -> {
                val next = now() + BACKOFF[(attempts - 1).coerceAtMost(BACKOFF.lastIndex)]
                repository.recordAttempt(delivery.id, DeliveryStatus.Retrying, attempts, result.statusCode, result.durationMs, result.error, next)
                noteFailure(hook)
            }
        }
    }

    /** A webhook failing for a whole day is switched off; the admin sees why in the portal. */
    private fun noteFailure(hook: StoredWebhook) {
        val since = hook.failingSince
        if (since == null) {
            repository.markFailing(hook.webhook.id, now())
        } else if (now() - since > DISABLE_AFTER) {
            repository.disable(hook.webhook.id, "Desativado automaticamente: falhou por mais de 24 horas.")
        }
    }

    private fun secretsOf(hook: StoredWebhook): List<String> {
        val previous = hook.previousSecret
        val until = hook.previousSecretUntil
        return if (previous != null && until != null && until > now()) listOf(hook.secret, previous) else listOf(hook.secret)
    }

    private fun now(): Instant = clock.instant().toKotlinInstant()

    private fun token(length: Int): String = String(CharArray(length) { ALPHABET[random.nextInt(ALPHABET.length)] })

    companion object {
        const val MAX_KEYS = 5
        const val RATE_PER_MINUTE = 60
        const val MAX_ATTEMPTS = 6
        private const val OUTBOX_BATCH = 100
        private const val RETRY_BATCH = 50
        private val OUTBOX_INTERVAL = 1_000L
        private val RETRY_INTERVAL = 15_000L
        private val OLD_SECRET_GRACE: Duration = 24.hours
        private val DISABLE_AFTER: Duration = 24.hours

        /** Wait after the 1st, 2nd... failed attempt: about 9 hours in all. */
        val BACKOFF: List<Duration> = listOf(1.minutes, 5.minutes, 30.minutes, 2.hours, 6.hours)

        private const val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

        /** The most specific event the webhook subscribed to, or null when none applies. */
        fun webhookEventFor(type: AppointmentEventType, subscribed: List<WebhookEvent>): WebhookEvent? {
            val specific = when (type) {
                AppointmentEventType.Created -> WebhookEvent.AppointmentCreated
                AppointmentEventType.Cancelled -> WebhookEvent.AppointmentCancelledByPatient
                AppointmentEventType.Rescheduled -> WebhookEvent.AppointmentRescheduled
                else -> null
            }
            return when {
                specific != null && specific in subscribed -> specific
                type != AppointmentEventType.Created && WebhookEvent.AppointmentStatusChanged in subscribed -> WebhookEvent.AppointmentStatusChanged
                else -> null
            }
        }
    }
}
