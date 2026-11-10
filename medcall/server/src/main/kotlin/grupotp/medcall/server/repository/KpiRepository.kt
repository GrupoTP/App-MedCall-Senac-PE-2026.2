package grupotp.medcall.server.repository

import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.clinic.EventOrigin
import grupotp.medcall.common.integration.DeliveryStatus
import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.server.database.table.AppointmentEventsTable
import grupotp.medcall.server.database.table.AppointmentsTable
import grupotp.medcall.server.database.table.ClinicProfilesTable
import grupotp.medcall.server.database.table.ClinicsTable
import grupotp.medcall.server.database.table.PreTriagesTable
import grupotp.medcall.server.database.table.ProfessionalProfilesTable
import grupotp.medcall.server.database.table.ProfessionalsTable
import grupotp.medcall.server.database.table.SpecialtiesTable
import grupotp.medcall.server.database.table.UsersTable
import grupotp.medcall.server.database.table.WebhookDeliveriesTable
import grupotp.medcall.server.database.table.WebhooksTable
import grupotp.medcall.server.database.wireName
import grupotp.medcall.server.database.wireValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

/** One appointment, reduced to what the KPIs read. No patient data besides an opaque id. */
data class AppointmentFact(
    val id: Int,
    val clinicId: Int,
    val professionalId: Int,
    val specialtyId: Int,
    val patientId: Int,
    val date: LocalDate,
    val time: LocalTime,
    val status: AppointmentStatus,
    val createdAt: Instant,
    val hasPreTriage: Boolean,
)

data class EventFact(
    val appointmentId: Int,
    val type: AppointmentEventType,
    val occurredAt: Instant,
    val origin: EventOrigin,
)

data class ProfessionalFact(
    val id: Int,
    val name: String,
    val clinicId: Int,
    val specialtyId: Int,
    val specialtyName: String,
    val active: Boolean,
)

data class ClinicFact(
    val id: Int,
    val name: String,
    val city: String,
    val timeZoneId: String,
    val status: ClinicStatus,
    val hasActiveWebhook: Boolean,
)

/** Everything a dashboard needs, read once per request. */
data class KpiFacts(
    val clinics: List<ClinicFact>,
    val professionals: List<ProfessionalFact>,
    val appointments: List<AppointmentFact>,
    /** Events of [appointments], oldest first. */
    val events: Map<Int, List<EventFact>>,
)

/**
 * Reads the raw facts of the KPIs. The arithmetic stays in Kotlin
 * ([grupotp.medcall.server.service.KpiService]), so it is the same on H2 and MySQL.
 */
class KpiRepository(
    private val database: Database,
) {
    /**
     * Appointments of [clinicIds] (all clinics when null) whose consultation date is in
     * [dates], or that were booked in [booked], plus every pending one from [pendingFrom] on.
     */
    fun facts(
        clinicIds: Collection<Int>?,
        dates: ClosedRange<LocalDate>,
        booked: ClosedRange<Instant>,
        pendingFrom: LocalDate,
    ): KpiFacts = transaction(database) {
        val clinics = clinics(clinicIds)
        val ids = clinics.map { it.id }
        val professionals = professionals(ids)
        val inPeriod: Op<Boolean> = AppointmentsTable.date.between(dates.start, dates.endInclusive) or
            AppointmentsTable.createdAt.between(booked.start, booked.endInclusive) or
            ((AppointmentsTable.status eq AppointmentStatus.Pending.wireName()) and (AppointmentsTable.date greaterEq pendingFrom))
        val rows = AppointmentsTable.innerJoin(ProfessionalsTable).selectAll()
            .where { (ProfessionalsTable.clinic inList ids) and inPeriod }
            .toList()
        // Same filter, joined: one scan each instead of thousands of ids in IN lists.
        val withPreTriage = PreTriagesTable.innerJoin(AppointmentsTable).innerJoin(ProfessionalsTable)
            .select(PreTriagesTable.appointment)
            .where { (ProfessionalsTable.clinic inList ids) and inPeriod }
            .mapNotNullTo(mutableSetOf()) { it[PreTriagesTable.appointment]?.value }
        val events = mutableMapOf<Int, MutableList<EventFact>>()
        AppointmentEventsTable.innerJoin(AppointmentsTable).innerJoin(ProfessionalsTable)
            .select(AppointmentEventsTable.appointment, AppointmentEventsTable.type, AppointmentEventsTable.occurredAt, AppointmentEventsTable.origin)
            .where { (ProfessionalsTable.clinic inList ids) and inPeriod }
            .forEach { row ->
                val id = row[AppointmentEventsTable.appointment].value
                events.getOrPut(id) { mutableListOf() } += EventFact(
                    appointmentId = id,
                    type = wireValue(row[AppointmentEventsTable.type]),
                    occurredAt = row[AppointmentEventsTable.occurredAt],
                    origin = wireValue(row[AppointmentEventsTable.origin]),
                )
            }
        events.values.forEach { list -> list.sortBy { it.occurredAt } }
        val appointments = rows.map { row ->
            val id = row[AppointmentsTable.id].value
            AppointmentFact(
                id = id,
                clinicId = row[ProfessionalsTable.clinic].value,
                professionalId = row[AppointmentsTable.professional].value,
                specialtyId = row[ProfessionalsTable.specialty].value,
                patientId = row[AppointmentsTable.patient].value,
                date = row[AppointmentsTable.date],
                time = row[AppointmentsTable.time],
                status = storedStatus(row[AppointmentsTable.status]),
                createdAt = row[AppointmentsTable.createdAt],
                hasPreTriage = id in withPreTriage,
            )
        }
        KpiFacts(clinics, professionals, appointments, events)
    }

    /** Patient sign-ups in [range], without accounts waiting for deletion. */
    fun signUps(range: ClosedRange<Instant>): List<Instant> = transaction(database) {
        UsersTable.select(UsersTable.createdAt)
            .where { UsersTable.createdAt.between(range.start, range.endInclusive) and UsersTable.deletionRequestedAt.isNull() }
            .map { it[UsersTable.createdAt] }
    }

    /** Latest booking time per clinic, over all time. */
    fun lastBookingByClinic(): Map<Int, Instant> = transaction(database) {
        val latest = AppointmentsTable.createdAt.max()
        AppointmentsTable.innerJoin(ProfessionalsTable)
            .select(ProfessionalsTable.clinic, latest)
            .groupBy(ProfessionalsTable.clinic)
            .mapNotNull { row -> row[latest]?.let { row[ProfessionalsTable.clinic].value to it } }
            .toMap()
    }

    /** Webhook deliveries created in [range]: (delivered, all, dead). */
    fun deliveries(range: ClosedRange<Instant>): Triple<Int, Int, Int> = transaction(database) {
        val statuses = WebhookDeliveriesTable.select(WebhookDeliveriesTable.status)
            .where { WebhookDeliveriesTable.createdAt.between(range.start, range.endInclusive) }
            .map { it[WebhookDeliveriesTable.status] }
        Triple(
            statuses.count { it == DeliveryStatus.Delivered.wireName() },
            statuses.size,
            statuses.count { it == DeliveryStatus.Dead.wireName() },
        )
    }

    private fun clinics(clinicIds: Collection<Int>?): List<ClinicFact> {
        val statuses = ClinicProfilesTable.selectAll().associate {
            it[ClinicProfilesTable.clinic].value to wireValue<ClinicStatus>(it[ClinicProfilesTable.status])
        }
        val withWebhook = WebhooksTable.select(WebhooksTable.clinic)
            .where { WebhooksTable.active eq true }
            .mapTo(mutableSetOf()) { it[WebhooksTable.clinic].value }
        val query = ClinicsTable.selectAll()
        if (clinicIds != null) {
            query.where { ClinicsTable.id inList clinicIds }
        }
        return query.map {
            val id = it[ClinicsTable.id].value
            ClinicFact(
                id = id,
                name = it[ClinicsTable.name],
                city = it[ClinicsTable.city],
                timeZoneId = it[ClinicsTable.timeZone],
                status = statuses[id] ?: ClinicStatus.Active,
                hasActiveWebhook = id in withWebhook,
            )
        }
    }

    private fun professionals(clinicIds: List<Int>): List<ProfessionalFact> {
        val inactive = ProfessionalProfilesTable.select(ProfessionalProfilesTable.professional)
            .where { ProfessionalProfilesTable.active eq false }
            .mapTo(mutableSetOf()) { it[ProfessionalProfilesTable.professional].value }
        return ProfessionalsTable.innerJoin(SpecialtiesTable).selectAll()
            .where { ProfessionalsTable.clinic inList clinicIds }
            .map {
                val id = it[ProfessionalsTable.id].value
                ProfessionalFact(
                    id = id,
                    name = it[ProfessionalsTable.name],
                    clinicId = it[ProfessionalsTable.clinic].value,
                    specialtyId = it[ProfessionalsTable.specialty].value,
                    specialtyName = it[SpecialtiesTable.name],
                    active = id !in inactive,
                )
            }
    }
}
