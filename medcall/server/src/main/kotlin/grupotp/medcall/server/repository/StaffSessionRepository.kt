package grupotp.medcall.server.repository

import grupotp.medcall.server.database.table.StaffSessionsTable
import grupotp.medcall.server.domain.StoredSession
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Instant

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

