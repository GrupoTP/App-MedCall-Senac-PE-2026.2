package grupotp.medcall.server.integration

import kotlinx.coroutines.CoroutineScope

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

