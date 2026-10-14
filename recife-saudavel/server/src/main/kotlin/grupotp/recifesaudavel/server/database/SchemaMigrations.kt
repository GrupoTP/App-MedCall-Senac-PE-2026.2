package grupotp.recifesaudavel.server.database

import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.vendors.currentDialectMetadata

/**
 * Brings databases created by older versions up to the current tables. Every step checks
 * the schema first, so running it on a new or already migrated database does nothing.
 * Only Exposed's own DDL is used, so the same code runs on H2 and MySQL.
 */
object SchemaMigrations {
    fun run(database: Database) {
        transaction(database) {
            addAppointmentSlotKey()
            addUserAccountDates()
        }
    }

    /** Adds the nullable `created_at`, `last_login_at` and `deletion_requested_at` columns to old `users` tables. */
    private fun JdbcTransaction.addUserAccountDates() {
        SchemaUtils.addMissingColumnsStatements(UsersTable).forEach { exec(it) }
    }

    /**
     * Moves the slot uniqueness from (professional, date, time) to `slot_key`, which is NULL
     * for cancelled appointments, so cancelling frees the slot.
     */
    private fun JdbcTransaction.addAppointmentSlotKey() {
        val table = AppointmentsTable
        // Adds the `slot_key` column together with its unique index.
        SchemaUtils.addMissingColumnsStatements(table).forEach { exec(it) }

        table.selectAll()
            .where { table.slotKey.isNull() and (table.status neq STATUS_CANCELLED) }
            .forEach { row ->
                val key = table.slotKeyOf(row[table.professionalId], row[table.appointmentDate], row[table.appointmentTime])
                table.update({ table.id eq row[table.id] }) { it[slotKey] = key }
            }

        // Found by its columns: H2 reports the name of the index behind the constraint, not the constraint's.
        val legacyColumns = setOf(table.professionalId, table.appointmentDate, table.appointmentTime)
        currentDialectMetadata.resetCaches()
        val hasLegacyIndex = currentDialectMetadata.existingIndices(table)[table].orEmpty()
            .any { it.unique && it.columns.toSet() == legacyColumns }
        if (hasLegacyIndex) {
            Index(legacyColumns.toList(), unique = true, customName = LEGACY_SLOT_INDEX)
                .dropStatement()
                .forEach { exec(it) }
            currentDialectMetadata.resetCaches()
        }
    }

    private const val STATUS_CANCELLED = "cancelled"

    /** The old unique index on (professional, date, time), which also blocked cancelled slots. */
    private const val LEGACY_SLOT_INDEX = "appointments_slot"
}
