package grupotp.recifesaudavel.server.database

import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.server.repository.NewAppointment
import grupotp.recifesaudavel.server.repository.SchedulingRepository
import grupotp.recifesaudavel.server.repository.Slot
import grupotp.recifesaudavel.server.repository.UserRepository
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaMigrationsTest {
    private val lucia = SchedulingCatalog.professionalId("Cardiologia", "Dra. Lúcia Oliveira")

    private lateinit var database: Database
    private lateinit var repository: SchedulingRepository
    private lateinit var patient: String

    @BeforeTest
    fun openLegacyDatabase() {
        database = DatabaseConnector().connect(
            DatabaseSettings(
                jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = "",
            ),
        )
        transaction(database) {
            SchemaUtils.create(UsersTable, SessionsTable, SpecialtiesTable, ProfessionalsTable, LegacyAppointmentsTable)
        }
        repository = SchedulingRepository(database).also { it.seedCatalogIfEmpty() }
        patient = UserRepository(database).insert(UUID.randomUUID().toString(), registration(), "hash")!!.id
    }

    @AfterTest
    fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun legacyAppointmentsGetASlotKeyAndCancelledSlotsAreFreed() {
        // Arrange
        val active = legacyAppointment(time = "09:00", status = "pending")
        val cancelled = legacyAppointment(time = "10:00", status = "cancelled")

        // Act
        SchemaMigrations.run(database)
        SchemaMigrations.run(database)
        val rebooked = repository.insert(newAppointment(time = "10:00"))
        val doubleBooked = repository.insert(newAppointment(time = "09:00"))

        // Assert
        assertEquals("$lucia|2026-10-13|09:00", slotKeyOf(active))
        assertNull(slotKeyOf(cancelled))
        assertTrue(rebooked)
        assertFalse(doubleBooked)
    }

    private fun legacyAppointment(time: String, status: String): String {
        val id = UUID.randomUUID().toString()
        transaction(database) {
            LegacyAppointmentsTable.insert {
                it[LegacyAppointmentsTable.id] = id
                it[patientId] = patient
                it[professionalId] = lucia
                it[appointmentDate] = "2026-10-13"
                it[appointmentTime] = time
                it[LegacyAppointmentsTable.status] = status
                it[reason] = ""
                it[notes] = ""
                it[createdAt] = 0
            }
        }
        return id
    }

    private fun slotKeyOf(id: String): String? = transaction(database) {
        AppointmentsTable.selectAll().where { AppointmentsTable.id eq id }.single()[AppointmentsTable.slotKey]
    }

    private fun newAppointment(time: String) = NewAppointment(
        id = UUID.randomUUID().toString(),
        patientId = patient,
        slot = Slot(lucia, "2026-10-13", time),
        reason = "",
        notes = "",
        createdAt = 0,
    )

    private fun registration() = RegisterAccountRequest(
        fullName = "Maria das Graças Silva",
        socialName = "",
        email = "maria@email.com",
        password = "segredo",
        primaryPhone = "(81) 99999-9999",
        primaryPhoneWhatsapp = true,
        secondaryPhone = "",
        secondaryPhoneWhatsapp = false,
        emergencyContactName = "",
        emergencyContactPhone = "",
        acceptedPrivacyPolicy = true,
        acceptedTreatment = true,
        wantsNotifications = false,
    )

    /** The `appointments` table as it was before `slot_key`. */
    private object LegacyAppointmentsTable : Table("appointments") {
        val id = varchar("id", 36)
        val patientId = varchar("patient_id", 36).references(UsersTable.id)
        val professionalId = varchar("professional_id", 36).references(ProfessionalsTable.id)
        val appointmentDate = varchar("appointment_date", 10)
        val appointmentTime = varchar("appointment_time", 5)
        val status = varchar("status", 20)
        val reason = varchar("reason", 1000)
        val notes = varchar("notes", 1000)
        val createdAt = long("created_at")

        override val primaryKey = PrimaryKey(id)

        init {
            uniqueIndex("appointments_slot", professionalId, appointmentDate, appointmentTime)
            index(false, patientId)
        }
    }
}
