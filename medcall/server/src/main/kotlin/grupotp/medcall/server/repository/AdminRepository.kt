package grupotp.medcall.server.repository

import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.admin.ManagedProfessional
import grupotp.medcall.common.admin.SaveClinicRequest
import grupotp.medcall.common.admin.SaveProfessionalRequest
import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.kpi.IntegrationMode
import grupotp.medcall.common.scheduling.Specialty
import grupotp.medcall.server.database.table.AppointmentsTable
import grupotp.medcall.server.database.table.ClinicProfilesTable
import grupotp.medcall.server.database.table.ClinicsTable
import grupotp.medcall.server.database.table.ProfessionalProfilesTable
import grupotp.medcall.server.database.table.ProfessionalsTable
import grupotp.medcall.server.database.table.SpecialtiesTable
import grupotp.medcall.server.database.table.WebhooksTable
import grupotp.medcall.server.database.wireName
import grupotp.medcall.server.database.wireValue
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Instant

/** Clinics, professionals and specialties as the MedCall panel manages them. */
class AdminRepository(
    private val database: Database,
    private val staff: StaffRepository,
) {
    fun clinics(): List<ManagedClinic> = transaction(database) {
        val profiles = ClinicProfilesTable.selectAll().associateBy { it[ClinicProfilesTable.clinic].value }
        val withWebhook = WebhooksTable.select(WebhooksTable.clinic).where { WebhooksTable.active eq true }
            .mapTo(mutableSetOf()) { it[WebhooksTable.clinic].value }
        val inactive = ProfessionalProfilesTable.selectAll().where { ProfessionalProfilesTable.active eq false }
            .mapTo(mutableSetOf()) { it[ProfessionalProfilesTable.professional].value }
        val professionals = ProfessionalsTable.selectAll()
            .filter { it[ProfessionalsTable.id].value !in inactive }
            .groupingBy { it[ProfessionalsTable.clinic].value }
            .eachCount()
        val admins = staff.activeAdminsByClinic()
        val latest = AppointmentsTable.createdAt.max()
        val lastBooking = AppointmentsTable.innerJoin(ProfessionalsTable)
            .select(ProfessionalsTable.clinic, latest)
            .groupBy(ProfessionalsTable.clinic)
            .mapNotNull { row -> row[latest]?.let { row[ProfessionalsTable.clinic].value to it } }
            .toMap()
        ClinicsTable.selectAll().orderBy(ClinicsTable.name to SortOrder.ASC).map {
            val id = it[ClinicsTable.id].value
            val profile = profiles[id]
            ManagedClinic(
                id = id,
                name = it[ClinicsTable.name],
                city = it[ClinicsTable.city],
                timeZoneId = it[ClinicsTable.timeZone],
                address = profile?.get(ClinicProfilesTable.address).orEmpty(),
                phone = profile?.get(ClinicProfilesTable.phone).orEmpty(),
                status = profile?.let { p -> wireValue<ClinicStatus>(p[ClinicProfilesTable.status]) } ?: ClinicStatus.Active,
                integrationMode = if (id in withWebhook) IntegrationMode.Webhook else IntegrationMode.Manual,
                professionals = professionals[id] ?: 0,
                admins = admins[id] ?: 0,
                lastAppointmentAt = lastBooking[id],
            )
        }
    }

    fun clinic(id: Int): ManagedClinic? = clinics().singleOrNull { it.id == id }

    /** New clinics start as drafts: patients only see them once active. */
    fun createClinic(request: SaveClinicRequest, now: Instant): ManagedClinic = transaction(database) {
        val id = ClinicsTable.insertAndGetId {
            it[name] = request.name.trim()
            it[city] = request.city.trim()
            it[timeZone] = request.timeZoneId
        }.value
        ClinicProfilesTable.insert {
            it[clinic] = id
            it[status] = ClinicStatus.Draft.wireName()
            it[address] = request.address.trim()
            it[phone] = request.phone.trim()
            it[createdAt] = now
        }
        id
    }.let { clinic(it)!! }

    fun updateClinic(id: Int, request: SaveClinicRequest, now: Instant): ManagedClinic? {
        val updated = transaction(database) {
            val count = ClinicsTable.update({ ClinicsTable.id eq id }) {
                it[name] = request.name.trim()
                it[city] = request.city.trim()
                it[timeZone] = request.timeZoneId
            }
            if (count == 0) return@transaction false
            ensureProfile(id, now)
            ClinicProfilesTable.update({ ClinicProfilesTable.clinic eq id }) {
                it[address] = request.address.trim()
                it[phone] = request.phone.trim()
            }
            true
        }
        return if (updated) clinic(id) else null
    }

    fun setClinicStatus(id: Int, status: ClinicStatus, now: Instant) {
        transaction(database) {
            ensureProfile(id, now)
            ClinicProfilesTable.update({ ClinicProfilesTable.clinic eq id }) { it[this.status] = status.wireName() }
        }
    }

    /** Clinics without a profile row are the active ones from before the panel; give them one. */
    private fun ensureProfile(id: Int, now: Instant) {
        if (ClinicProfilesTable.selectAll().where { ClinicProfilesTable.clinic eq id }.empty()) {
            ClinicProfilesTable.insert {
                it[clinic] = id
                it[status] = ClinicStatus.Active.wireName()
                it[createdAt] = now
            }
        }
    }

    fun specialties(): List<Specialty> = transaction(database) {
        SpecialtiesTable.selectAll().orderBy(SpecialtiesTable.name to SortOrder.ASC)
            .map { Specialty(it[SpecialtiesTable.id].value, it[SpecialtiesTable.name]) }
    }

    /** Null when the name is taken. */
    fun createSpecialty(name: String): Specialty? = try {
        transaction(database) {
            val id = SpecialtiesTable.insertAndGetId { it[this.name] = name }.value
            Specialty(id, name)
        }
    } catch (error: ExposedSQLException) {
        if (!error.isUniqueViolation()) throw error
        null
    }

    fun professionals(clinicId: Int?): List<ManagedProfessional> = transaction(database) {
        val profiles = ProfessionalProfilesTable.selectAll().associateBy { it[ProfessionalProfilesTable.professional].value }
        ProfessionalsTable.innerJoin(SpecialtiesTable).selectAll()
            .apply { if (clinicId != null) where { ProfessionalsTable.clinic eq clinicId } }
            .orderBy(ProfessionalsTable.name to SortOrder.ASC)
            .map {
                val id = it[ProfessionalsTable.id].value
                ManagedProfessional(
                    id = id,
                    name = it[ProfessionalsTable.name],
                    registration = profiles[id]?.get(ProfessionalProfilesTable.registration).orEmpty(),
                    specialtyId = it[ProfessionalsTable.specialty].value,
                    specialtyName = it[SpecialtiesTable.name],
                    clinicId = it[ProfessionalsTable.clinic].value,
                    active = profiles[id]?.get(ProfessionalProfilesTable.active) ?: true,
                )
            }
    }

    fun professional(id: Int): ManagedProfessional? = professionals(null).singleOrNull { it.id == id }

    fun createProfessional(request: SaveProfessionalRequest): ManagedProfessional = transaction(database) {
        val id = ProfessionalsTable.insertAndGetId {
            it[name] = request.name.trim()
            it[specialty] = request.specialtyId
            it[clinic] = request.clinicId
        }.value
        ProfessionalProfilesTable.insert {
            it[professional] = id
            it[registration] = request.registration.trim()
            it[active] = request.active
        }
        id
    }.let { professional(it)!! }

    fun updateProfessional(id: Int, request: SaveProfessionalRequest): ManagedProfessional? {
        val found = transaction(database) {
            val count = ProfessionalsTable.update({ ProfessionalsTable.id eq id }) {
                it[name] = request.name.trim()
                it[specialty] = request.specialtyId
                it[clinic] = request.clinicId
            }
            if (count == 0) return@transaction false
            if (ProfessionalProfilesTable.selectAll().where { ProfessionalProfilesTable.professional eq id }.empty()) {
                ProfessionalProfilesTable.insert {
                    it[professional] = id
                    it[registration] = request.registration.trim()
                    it[active] = request.active
                }
            } else {
                ProfessionalProfilesTable.update({ ProfessionalProfilesTable.professional eq id }) {
                    it[registration] = request.registration.trim()
                    it[active] = request.active
                }
            }
            true
        }
        return if (found) professional(id) else null
    }
}
