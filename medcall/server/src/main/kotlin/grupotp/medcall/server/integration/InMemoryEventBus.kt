package grupotp.medcall.server.integration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel as KChannel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

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

