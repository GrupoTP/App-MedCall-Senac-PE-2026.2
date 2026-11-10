package grupotp.medcall.server.integration

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel as KChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/** What travels on the bus: an appointment event of a clinic. */
@Serializable
data class BusEvent(
    val eventId: String,
    val clinicId: Int,
    val appointmentId: Int,
    /** Wire name of AppointmentEventType. */
    val type: String,
    val occurredAtEpochMs: Long,
)

/**
 * Carries appointment events from the outbox to the webhook dispatcher. RabbitMQ in a deployment
 * with `RABBITMQ_URL`; an in-process channel otherwise (development and tests), with the same
 * interface, so the rest of the server does not know which one runs.
 */
interface EventBus : AutoCloseable {
    val name: String

    /** Returns only once the broker accepted the message; throws otherwise, and the outbox keeps it. */
    fun publish(event: BusEvent)

    /** Hands each event to [handler]; an exception sends the message to the dead-letter queue. */
    fun subscribe(scope: CoroutineScope, handler: suspend (BusEvent) -> Unit)

    /** Records a message that exhausted its retries. */
    fun deadLetter(event: BusEvent, reason: String)

    override fun close() = Unit
}

/** Development bus: a channel in the server's own memory. Events are lost on restart (the outbox is not). */
class InMemoryEventBus : EventBus {
    private val log = LoggerFactory.getLogger(InMemoryEventBus::class.java)
    private val channel = KChannel<BusEvent>(KChannel.UNLIMITED)
    override val name = "memória"

    override fun publish(event: BusEvent) {
        channel.trySend(event).getOrThrow()
    }

    override fun subscribe(scope: CoroutineScope, handler: suspend (BusEvent) -> Unit) {
        scope.launch {
            for (event in channel) {
                runCatching { handler(event) }.onFailure { log.warn("Event {} failed: {}", event.eventId, it.message) }
            }
        }
    }

    override fun deadLetter(event: BusEvent, reason: String) {
        log.warn("Dead letter {} ({}): {}", event.eventId, event.type, reason)
    }
}

/**
 * RabbitMQ: topic exchange `medcall.appointments` (routing key = event type), durable queue
 * `webhook.delivery` bound to every key, and `webhook.delivery.dlq` for what could not be handled.
 * Messages are persistent and acknowledged only after the handler stored the deliveries.
 */
class RabbitMqEventBus(url: String) : EventBus {
    private val log = LoggerFactory.getLogger(RabbitMqEventBus::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val connection: Connection = ConnectionFactory().apply {
        setUri(url)
        isAutomaticRecoveryEnabled = true
    }.newConnection("medcall-server")
    private val publishChannel: Channel = connection.createChannel().also { declare(it) }
    override val name = "RabbitMQ"

    private fun declare(channel: Channel) {
        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, true)
        channel.queueDeclare(DLQ, true, false, false, null)
        channel.queueDeclare(QUEUE, true, false, false, mapOf("x-dead-letter-exchange" to "", "x-dead-letter-routing-key" to DLQ))
        channel.queueBind(QUEUE, EXCHANGE, "#")
        channel.confirmSelect()
    }

    @Synchronized
    override fun publish(event: BusEvent) {
        val properties = AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(PERSISTENT)
            .messageId(event.eventId)
            .build()
        publishChannel.basicPublish(EXCHANGE, event.type, properties, json.encodeToString(event).toByteArray())
        publishChannel.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS)
    }

    override fun subscribe(scope: CoroutineScope, handler: suspend (BusEvent) -> Unit) {
        val channel = connection.createChannel()
        channel.basicQos(PREFETCH)
        val deliver = DeliverCallback { _, message ->
            val tag = message.envelope.deliveryTag
            scope.launch {
                try {
                    handler(json.decodeFromString<BusEvent>(String(message.body)))
                    channel.basicAck(tag, false)
                } catch (error: Exception) {
                    log.warn("Event failed, sent to {}: {}", DLQ, error.message)
                    channel.basicNack(tag, false, false)
                }
            }
        }
        channel.basicConsume(QUEUE, false, deliver, CancelCallback { })
    }

    @Synchronized
    override fun deadLetter(event: BusEvent, reason: String) {
        val properties = AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(PERSISTENT)
            .messageId(event.eventId)
            .headers(mapOf("x-reason" to reason.take(200)))
            .build()
        runCatching { publishChannel.basicPublish("", DLQ, properties, json.encodeToString(event).toByteArray()) }
            .onFailure { log.warn("Could not publish dead letter {}: {}", event.eventId, it.message) }
    }

    override fun close() {
        runCatching { connection.close() }
    }

    private companion object {
        const val EXCHANGE = "medcall.appointments"
        const val QUEUE = "webhook.delivery"
        const val DLQ = "webhook.delivery.dlq"
        const val PERSISTENT = 2
        const val PREFETCH = 20
        const val CONFIRM_TIMEOUT_MS = 5_000L
    }
}
