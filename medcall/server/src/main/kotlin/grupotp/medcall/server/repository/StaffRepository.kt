package grupotp.medcall.server.repository

import grupotp.medcall.common.admin.AuditEntry
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.server.database.table.ApiKeysTable
import grupotp.medcall.server.database.table.AuditLogTable
import grupotp.medcall.server.database.table.ClinicsTable
import grupotp.medcall.server.database.table.StaffSessionsTable
import grupotp.medcall.server.database.table.StaffUsersTable
import grupotp.medcall.server.database.wireName
import grupotp.medcall.server.database.wireValue
import grupotp.medcall.server.domain.StoredSession
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Instant

/** A staff account with what only the server reads. */
data class StoredStaff(
    val user: StaffUser,
    val passwordHash: String,
    val clinicName: String?,
)

/** Changes to a clinic user, checked together in one transaction. */
sealed interface StaffUpdateOutcome {
    data class Updated(val user: StaffUser) : StaffUpdateOutcome
    data object NotFound : StaffUpdateOutcome
    data object LastAdmin : StaffUpdateOutcome
}

class StaffRepository(
    private val database: Database,
) {
    fun isEmpty(): Boolean = transaction(database) { StaffUsersTable.selectAll().limit(1).empty() }

    fun findByEmail(email: String): StoredStaff? = transaction(database) {
        withClinic().where { StaffUsersTable.email eq email }.singleOrNull()?.toStored()
    }

    fun findById(id: Int): StoredStaff? = transaction(database) {
        withClinic().where { StaffUsersTable.id eq id }.singleOrNull()?.toStored()
    }

    /** Returns null when the e-mail is already taken. */
    fun insert(
        fullName: String,
        email: String,
        role: StaffRole,
        clinicId: Int?,
        passwordHash: String,
        mustChangePassword: Boolean,
        now: Instant,
    ): StaffUser? = try {
        transaction(database) {
            val id = StaffUsersTable.insertAndGetId {
                it[this.fullName] = fullName
                it[this.email] = email
                it[this.role] = role.wireName()
                it[clinic] = clinicId
                it[this.passwordHash] = passwordHash
                it[active] = true
                it[this.mustChangePassword] = mustChangePassword
                it[createdAt] = now
            }.value
            withClinic().where { StaffUsersTable.id eq id }.single().toStored().user
        }
    } catch (error: ExposedSQLException) {
        if (!error.isUniqueViolation()) {
            throw error
        }
        null
    }

    /** Clinic staff of [clinicId], or every clinic staff member when null. MedCall's team is never listed. */
    fun listClinicStaff(clinicId: Int?): List<StaffUser> = transaction(database) {
        withClinic()
            .where {
                if (clinicId == null) StaffUsersTable.clinic.isNotNull() else StaffUsersTable.clinic eq clinicId
            }
            .orderBy(StaffUsersTable.fullName to SortOrder.ASC)
            .map { it.toStored().user }
    }

    /**
     * Applies the non-null changes to user [id] of [clinicId]. Refuses to leave the clinic without
     * an active administrator; the clinic's rows are locked while checking, so two admins demoting
     * each other at once cannot both succeed.
     */
    fun updateClinicUser(clinicId: Int, id: Int, fullName: String?, role: StaffRole?, active: Boolean?): StaffUpdateOutcome =
        transaction(database) {
            val staff = StaffUsersTable.selectAll().where { StaffUsersTable.clinic eq clinicId }.forUpdate().toList()
            val current = staff.singleOrNull { it[StaffUsersTable.id].value == id } ?: return@transaction StaffUpdateOutcome.NotFound
            val newRole = role ?: wireValue(current[StaffUsersTable.role])
            val newActive = active ?: current[StaffUsersTable.active]
            val adminsLeft = staff.count { row ->
                if (row[StaffUsersTable.id].value == id) {
                    newRole == StaffRole.ClinicAdmin && newActive
                } else {
                    row[StaffUsersTable.role] == StaffRole.ClinicAdmin.wireName() && row[StaffUsersTable.active]
                }
            }
            if (adminsLeft == 0) {
                return@transaction StaffUpdateOutcome.LastAdmin
            }
            StaffUsersTable.update({ StaffUsersTable.id eq id }) {
                if (fullName != null) it[this.fullName] = fullName
                it[this.role] = newRole.wireName()
                it[this.active] = newActive
            }
            StaffUpdateOutcome.Updated(withClinic().where { StaffUsersTable.id eq id }.single().toStored().user)
        }

    /** For MedCall's own team: switches any clinic account on or off, without the last-admin rule. */
    fun setActive(id: Int, active: Boolean): Boolean = transaction(database) {
        StaffUsersTable.update({ (StaffUsersTable.id eq id) and StaffUsersTable.clinic.isNotNull() }) {
            it[this.active] = active
        } > 0
    }

    fun setPassword(id: Int, passwordHash: String, mustChangePassword: Boolean) {
        transaction(database) {
            StaffUsersTable.update({ StaffUsersTable.id eq id }) {
                it[this.passwordHash] = passwordHash
                it[this.mustChangePassword] = mustChangePassword
            }
        }
    }

    fun recordLogin(id: Int, at: Instant) {
        transaction(database) {
            StaffUsersTable.update({ StaffUsersTable.id eq id }) { it[lastLoginAt] = at }
        }
    }

    /** Active administrators per clinic. */
    fun activeAdminsByClinic(): Map<Int, Int> = transaction(database) {
        StaffUsersTable.selectAll()
            .where { (StaffUsersTable.role eq StaffRole.ClinicAdmin.wireName()) and (StaffUsersTable.active eq true) }
            .mapNotNull { it[StaffUsersTable.clinic]?.value }
            .groupingBy { it }
            .eachCount()
    }

    private fun withClinic() = StaffUsersTable.join(ClinicsTable, JoinType.LEFT, StaffUsersTable.clinic, ClinicsTable.id).selectAll()

    private fun ResultRow.toStored(): StoredStaff = StoredStaff(
        user = StaffUser(
            id = this[StaffUsersTable.id].value,
            fullName = this[StaffUsersTable.fullName],
            email = this[StaffUsersTable.email],
            role = wireValue(this[StaffUsersTable.role]),
            clinicId = this[StaffUsersTable.clinic]?.value,
            active = this[StaffUsersTable.active],
            mustChangePassword = this[StaffUsersTable.mustChangePassword],
            createdAt = this[StaffUsersTable.createdAt],
            lastLoginAt = this[StaffUsersTable.lastLoginAt],
        ),
        passwordHash = this[StaffUsersTable.passwordHash],
        clinicName = this.getOrNull(ClinicsTable.name),
    )
}

/** [SessionStore] of the staff portals. */
class StaffSessionRepository(
    private val database: Database,
) : SessionStore {
    override fun insert(userId: Int, createdAt: Instant): Int = transaction(database) {
        StaffSessionsTable.insertAndGetId {
            it[staffUser] = userId
            it[this.createdAt] = createdAt
            it[lastUsedAt] = createdAt
        }.value
    }

    override fun find(id: Int): StoredSession? = transaction(database) {
        StaffSessionsTable.selectAll().where { StaffSessionsTable.id eq id }.singleOrNull()?.let {
            StoredSession(
                userId = it[StaffSessionsTable.staffUser].value,
                createdAt = it[StaffSessionsTable.createdAt],
                lastUsedAt = it[StaffSessionsTable.lastUsedAt],
            )
        }
    }

    override fun touch(id: Int, at: Instant) {
        transaction(database) {
            StaffSessionsTable.update({ StaffSessionsTable.id eq id }) { it[lastUsedAt] = at }
        }
    }

    override fun delete(id: Int) {
        transaction(database) {
            StaffSessionsTable.deleteWhere { StaffSessionsTable.id eq id }
        }
    }

    override fun deleteAllOf(userId: Int, keepId: Int?) {
        transaction(database) {
            StaffSessionsTable.deleteWhere {
                if (keepId == null) {
                    StaffSessionsTable.staffUser eq userId
                } else {
                    (StaffSessionsTable.staffUser eq userId) and (StaffSessionsTable.id neq keepId)
                }
            }
        }
    }

    override fun deleteUnusedSince(cutoff: Instant) {
        transaction(database) {
            StaffSessionsTable.deleteWhere { StaffSessionsTable.lastUsedAt less cutoff }
        }
    }
}

/** Who did what in the portals. */
class AuditRepository(
    private val database: Database,
) {
    fun record(at: Instant, action: String, target: String, clinicId: Int?, staffUserId: Int? = null, apiKeyId: Int? = null) {
        transaction(database) {
            AuditLogTable.insert {
                it[occurredAt] = at
                it[this.action] = action
                it[this.target] = target.take(200)
                it[clinic] = clinicId
                it[staffUser] = staffUserId
                it[apiKey] = apiKeyId
            }
        }
    }

    /** Newest first. */
    fun list(clinicId: Int?, limit: Int): List<AuditEntry> = transaction(database) {
        AuditLogTable
            .join(StaffUsersTable, JoinType.LEFT, AuditLogTable.staffUser, StaffUsersTable.id)
            .join(ApiKeysTable, JoinType.LEFT, AuditLogTable.apiKey, ApiKeysTable.id)
            .selectAll()
            .apply { if (clinicId != null) where { AuditLogTable.clinic eq clinicId } }
            .orderBy(AuditLogTable.id to SortOrder.DESC)
            .limit(limit)
            .map {
                AuditEntry(
                    occurredAt = it[AuditLogTable.occurredAt],
                    actorName = it.getOrNull(StaffUsersTable.fullName)
                        ?: it.getOrNull(ApiKeysTable.name)?.let { name -> "Chave de API \"$name\"" }
                        ?: "Sistema",
                    action = it[AuditLogTable.action],
                    target = it[AuditLogTable.target],
                    clinicId = it[AuditLogTable.clinic]?.value,
                )
            }
    }
}
