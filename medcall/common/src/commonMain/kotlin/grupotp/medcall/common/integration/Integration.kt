package grupotp.medcall.common.integration

import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.security.SecretString
import grupotp.medcall.common.scheduling.AppointmentStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Events a clinic can subscribe to. The wire name is what goes in the webhook's `type`. */
@Serializable
enum class WebhookEvent(val wireName: String, val label: String) {
    @SerialName("appointment.created")
    AppointmentCreated("appointment.created", "Novo agendamento"),

    @SerialName("appointment.cancelled_by_patient")
    AppointmentCancelledByPatient("appointment.cancelled_by_patient", "Cancelado pelo paciente"),

    @SerialName("appointment.rescheduled")
    AppointmentRescheduled("appointment.rescheduled", "Remarcado"),

    @SerialName("appointment.status_changed")
    AppointmentStatusChanged("appointment.status_changed", "Qualquer mudança de situação"),
}

@Serializable
data class Webhook(
    val id: Int,
    val url: String,
    val description: String = "",
    val events: List<WebhookEvent>,
    val active: Boolean,
    val createdAt: Instant,
    /** Set when the server switched it off after failing for 24 hours. */
    val disabledReason: String = "",
    val secretRotatedAt: Instant? = null,
)

@Serializable
data class SaveWebhookRequest(
    val url: String,
    val description: String = "",
    val events: List<WebhookEvent>,
    val active: Boolean = true,
)

/** A new webhook or a rotated secret. [secret] is shown once. */
@Serializable
data class WebhookWithSecret(
    val webhook: Webhook,
    val secret: SecretString,
)

@Serializable
enum class DeliveryStatus {
    @SerialName("pending") Pending,
    @SerialName("delivered") Delivered,
    @SerialName("retrying") Retrying,
    @SerialName("dead") Dead,
}

@Serializable
data class WebhookDelivery(
    val id: Int,
    val webhookId: Int,
    val eventId: String,
    val eventType: String,
    val status: DeliveryStatus,
    val attempts: Int,
    val lastStatusCode: Int? = null,
    val lastDurationMs: Long? = null,
    val lastError: String = "",
    val createdAt: Instant,
    val nextAttemptAt: Instant? = null,
)

/** Result of the "Enviar teste" button. */
@Serializable
data class WebhookTestResult(
    val statusCode: Int?,
    val durationMs: Long,
    val error: String = "",
)

@Serializable
data class ApiKey(
    val id: Int,
    val name: String,
    val lastFour: String,
    val createdAt: Instant,
    val createdBy: String,
    val lastUsedAt: Instant? = null,
    val revokedAt: Instant? = null,
)

@Serializable
data class CreateApiKeyRequest(
    val name: String,
)

/** [key] is shown once; the server keeps only its hash. */
@Serializable
data class ApiKeyWithSecret(
    val apiKey: ApiKey,
    val key: SecretString,
)

/**
 * The body MedCall posts to a clinic's webhook. Delivery is at least once: the clinic drops
 * repeats by [eventId].
 */
@Serializable
data class WebhookPayload(
    val eventId: String,
    val type: String,
    val occurredAt: Instant,
    val clinicId: Int,
    val appointment: PublicAppointment,
)

/** An appointment as the public API and the webhooks show it to the clinic's own system. */
@Serializable
data class PublicAppointment(
    val id: Int,
    val status: AppointmentStatus,
    val date: LocalDate,
    val time: LocalTime,
    val professionalId: Int,
    val professionalName: String,
    val specialtyName: String,
    val patientName: String,
    val patientPhone: String,
    val reason: String = "",
    val createdAt: Instant,
    val lastEvent: AppointmentEventType? = null,
)
