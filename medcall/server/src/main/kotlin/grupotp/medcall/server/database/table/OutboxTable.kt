package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

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

