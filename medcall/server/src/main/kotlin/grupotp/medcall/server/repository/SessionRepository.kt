package grupotp.medcall.server.repository

import grupotp.medcall.server.database.entity.SessionEntity
import grupotp.medcall.server.database.entity.UserEntity
import grupotp.medcall.server.database.table.SessionsTable
import grupotp.medcall.server.domain.StoredSession
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

/** Single sessions go through [SessionEntity]; deleting many at once stays one SQL statement. */
/** Where [grupotp.medcall.server.service.SessionService] keeps sessions: patients' or staff's. */
interface SessionStore {
    fun insert(userId: Int, createdAt: Instant): Int
    fun find(id: Int): StoredSession?
    fun touch(id: Int, at: Instant)
    fun delete(id: Int)
    fun deleteAllOf(userId: Int, keepId: Int? = null)
    fun deleteUnusedSince(cutoff: Instant)
}

class SessionRepository(
    private val database: Database,
) : SessionStore {
    /** Returns the id of the new session. */
    override fun insert(userId: Int, createdAt: Instant): Int = transaction(database) {
        SessionEntity.new {
            user = UserEntity[userId]
            this.createdAt = createdAt
            lastUsedAt = createdAt
        }.id.value
    }

    override fun find(id: Int): StoredSession? = transaction(database) {
        SessionEntity.findById(id)?.let {
            StoredSession(
                userId = it.userId.value,
                createdAt = it.createdAt,
                lastUsedAt = it.lastUsedAt
            )
        }
    }

    override fun touch(id: Int, at: Instant) {
        transaction(database) {
            SessionEntity.findById(id)?.lastUsedAt = at
        }
    }

    override fun delete(id: Int) {
        transaction(database) {
            SessionEntity.findById(id)?.delete()
        }
    }

    /** Deletes every session of [userId] except [keepId], if given. */
    override fun deleteAllOf(userId: Int, keepId: Int?) {
        transaction(database) {
            SessionsTable.deleteWhere {
                if (keepId == null) {
                    SessionsTable.user eq userId
                } else {
                    (SessionsTable.user eq userId) and (SessionsTable.id neq keepId)
                }
            }
        }
    }

    /** Deletes the sessions whose last use is before [cutoff]. */
    override fun deleteUnusedSince(cutoff: Instant) {
        transaction(database) {
            SessionsTable.deleteWhere { SessionsTable.lastUsedAt less cutoff }
        }
    }
}
