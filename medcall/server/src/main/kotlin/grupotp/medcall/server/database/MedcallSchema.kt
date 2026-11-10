package grupotp.medcall.server.database

import grupotp.medcall.server.database.table.ApiKeysTable
import grupotp.medcall.server.database.table.AppointmentEventsTable
import grupotp.medcall.server.database.table.AppointmentsTable
import grupotp.medcall.server.database.table.AuditLogTable
import grupotp.medcall.server.database.table.ClinicProfilesTable
import grupotp.medcall.server.database.table.ClinicsTable
import grupotp.medcall.server.database.table.IdempotencyKeysTable
import grupotp.medcall.server.database.table.OutboxTable
import grupotp.medcall.server.database.table.PreTriagesTable
import grupotp.medcall.server.database.table.ProfessionalProfilesTable
import grupotp.medcall.server.database.table.ProfessionalsTable
import grupotp.medcall.server.database.table.SessionsTable
import grupotp.medcall.server.database.table.SpecialtiesTable
import grupotp.medcall.server.database.table.StaffSessionsTable
import grupotp.medcall.server.database.table.StaffUsersTable
import grupotp.medcall.server.database.table.UsersTable
import grupotp.medcall.server.database.table.WebhookDeliveriesTable
import grupotp.medcall.server.database.table.WebhooksTable
import org.jetbrains.exposed.v1.core.Table

/** Every table of the server, in an order where references come first. Tests create the same list. */
object MedcallSchema {
    val tables: Array<Table> = arrayOf(
        UsersTable,
        SessionsTable,
        ClinicsTable,
        SpecialtiesTable,
        ProfessionalsTable,
        AppointmentsTable,
        PreTriagesTable,
        StaffUsersTable,
        StaffSessionsTable,
        ApiKeysTable,
        AppointmentEventsTable,
        AuditLogTable,
        ClinicProfilesTable,
        ProfessionalProfilesTable,
        WebhooksTable,
        OutboxTable,
        WebhookDeliveriesTable,
        IdempotencyKeysTable,
    )
}
