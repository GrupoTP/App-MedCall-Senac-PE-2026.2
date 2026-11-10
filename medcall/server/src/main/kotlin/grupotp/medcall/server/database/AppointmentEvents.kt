package grupotp.medcall.server.database

import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.clinic.EventOrigin
import grupotp.medcall.server.database.table.AppointmentEventsTable
import grupotp.medcall.server.database.table.OutboxTable
import grupotp.medcall.server.domain.Slot
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.UUID
import kotlin.time.Instant

/**
 * `@SerialName`s of each enum, by ordinal. Looking the serializer up costs a reflective call, and
 * the dashboards convert tens of thousands of rows, so each enum is read once.
 */
@PublishedApi
internal val wireNamesCache = java.util.concurrent.ConcurrentHashMap<Class<*>, Array<String>>()

@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T : Enum<T>> wireNames(): Array<String> = wireNamesCache.getOrPut(T::class.java) {
    val descriptor = serializer<T>().descriptor
    Array(descriptor.elementsCount) { descriptor.getElementName(it) }
}

/** The `@SerialName` of an enum value: the same text in JSON and in the database. */
inline fun <reified T : Enum<T>> T.wireName(): String = wireNames<T>()[ordinal]

/** Reverse of [wireName]. Throws on an unknown name: the database only holds names written by [wireName]. */
inline fun <reified T : Enum<T>> wireValue(name: String): T {
    val index = wireNames<T>().indexOf(name)
    require(index >= 0) { "Unknown ${T::class.simpleName} '$name'." }
    return enumValues<T>()[index]
}

/** Who caused a change. Exactly one of [staffUserId] and [apiKeyId] is set for clinic changes. */
data class Actor(
    val origin: EventOrigin,
    val staffUserId: Int? = null,
    val apiKeyId: Int? = null,
) {
    companion object {
        val PATIENT = Actor(EventOrigin.PatientApp)
    }
}

/**
 * Writes one appointment event and, for what clinics can subscribe to, its outbox row. Call it
 * inside the transaction that changes the appointment, so the change and its event commit together.
 */
fun recordAppointmentEvent(
    appointmentId: Int,
    clinicId: Int,
    type: AppointmentEventType,
    actor: Actor,
    at: Instant,
    detail: String = "",
    previous: Slot? = null,
) {
    AppointmentEventsTable.insert {
        it[appointment] = appointmentId
        it[clinic] = clinicId
        it[this.type] = type.wireName()
        it[occurredAt] = at
        it[origin] = actor.origin.wireName()
        it[staffUser] = actor.staffUserId
        it[apiKey] = actor.apiKeyId
        it[this.detail] = detail.take(300)
        it[previousDate] = previous?.date
        it[previousTime] = previous?.time
    }
    if (actor.origin != EventOrigin.DemoSeed) {
        OutboxTable.insert {
            it[eventId] = UUID.randomUUID().toString()
            it[clinic] = clinicId
            it[appointment] = appointmentId
            it[eventType] = type.wireName()
            it[occurredAt] = at
        }
    }
}
