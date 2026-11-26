package grupotp.medcall.server.repository

import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.scheduling.AppointmentStatus
import grupotp.medcall.server.database.table.AppointmentEventsTable
import grupotp.medcall.server.database.table.ClinicProfilesTable
import grupotp.medcall.server.database.table.ProfessionalProfilesTable
import grupotp.medcall.server.database.table.ProfessionalsTable
import grupotp.medcall.server.database.wireName
import grupotp.medcall.server.database.wireValue
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/*
 * Pieces shared by the repositories that read appointments. All of them run inside the caller's
 * transaction.
 */

internal fun AppointmentStatus.stored(): String = wireName()

internal fun storedStatus(value: String): AppointmentStatus = wireValue(value)


/** Statuses that hold the slot, as stored. */
internal val SLOT_HOLDING_STATUSES: List<String> = AppointmentStatus.entries.filter { it.holdsSlot }.map { it.stored() }


/** [AppointmentExtras] of each of [appointmentIds], in one query. */
internal fun appointmentExtras(appointmentIds: Collection<Int>): Map<Int, AppointmentExtras> {
    if (appointmentIds.isEmpty()) {
        return emptyMap()
    }
    val types = listOf(AppointmentEventType.Rescheduled.wireName(), AppointmentEventType.Declined.wireName())
    val result = mutableMapOf<Int, AppointmentExtras>()
    appointmentIds.chunked(IN_LIST_CHUNK).forEach { chunk ->
        AppointmentEventsTable
            .select(AppointmentEventsTable.appointment, AppointmentEventsTable.type, AppointmentEventsTable.detail)
            .where { (AppointmentEventsTable.appointment inList chunk) and (AppointmentEventsTable.type inList types) }
            .orderBy(AppointmentEventsTable.id to SortOrder.ASC)
            .forEach { row ->
                val id = row[AppointmentEventsTable.appointment].value
                val current = result[id] ?: AppointmentExtras()
                result[id] = when (row[AppointmentEventsTable.type]) {
                    AppointmentEventType.Rescheduled.wireName() -> current.copy(rescheduled = true)
                    else -> current.copy(declineReason = DeclineDetail.text(row[AppointmentEventsTable.detail]))
                }
            }
    }
    return result
}


/** Professionals patients cannot book: switched off, or at a clinic that is not active. */
internal fun hiddenProfessionalIds(): Set<Int> {
    val closedClinics = ClinicProfilesTable.select(ClinicProfilesTable.clinic)
        .where { ClinicProfilesTable.status neq ClinicStatus.Active.wireName() }
        .map { it[ClinicProfilesTable.clinic].value }
    val inactive = ProfessionalProfilesTable.select(ProfessionalProfilesTable.professional)
        .where { ProfessionalProfilesTable.active eq false }
        .mapTo(mutableSetOf()) { it[ProfessionalProfilesTable.professional].value }
    if (closedClinics.isNotEmpty()) {
        ProfessionalsTable.selectAll().where { ProfessionalsTable.clinic inList closedClinics }
            .forEach { inactive += it[ProfessionalsTable.id].value }
    }
    return inactive
}


/** Clinics whose status is not active. Clinics without a profile row are active. */
internal fun closedClinicIds(): Set<Int> =
    ClinicProfilesTable.select(ClinicProfilesTable.clinic)
        .where { ClinicProfilesTable.status neq ClinicStatus.Active.wireName() }
        .mapTo(mutableSetOf()) { it[ClinicProfilesTable.clinic].value }


/** Keeps `IN (...)` lists well under every database's parameter limit. */
internal const val IN_LIST_CHUNK = 1000
