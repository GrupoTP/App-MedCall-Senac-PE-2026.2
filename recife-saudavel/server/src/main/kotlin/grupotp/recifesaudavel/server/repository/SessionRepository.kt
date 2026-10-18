package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.server.database.table.SessionsTable
import grupotp.recifesaudavel.server.database.entity.SessionEntity
import grupotp.recifesaudavel.server.database.entity.UserEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

data class StoredSession(
    val userId: Int,
    val createdAt: Instant,
    val lastUsedAt: Instant,
)

/** Single sessions go through [SessionEntity]; deleting many at once stays one SQL statement. */
class SessionRepository(
    private val database: Database,
) {
    /** Returns the id of the new session. */
    fun insert(userId: Int, createdAt: Instant): Int = transaction(database) {
        SessionEntity.new {
            user = UserEntity[userId]
            this.createdAt = createdAt
            lastUsedAt = createdAt
        }.id.value
    }

    fun find(id: Int): StoredSession? = transaction(database) {
        SessionEntity.findById(id)?.let {
            StoredSession(userId = it.userId.value, createdAt = it.createdAt, lastUsedAt = it.lastUsedAt)
        }
    }

    fun touch(id: Int, at: Instant) {
        transaction(database) {
            SessionEntity.findById(id)?.lastUsedAt = at
        }
    }

    fun delete(id: Int) {
        transaction(database) {
            SessionEntity.findById(id)?.delete()
        }
    }

    /** Deletes every session of [userId] except [keepId], if given. */
    fun deleteAllOf(userId: Int, keepId: Int? = null) {
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
    fun deleteUnusedSince(cutoff: Instant) {
        transaction(database) {
            SessionsTable.deleteWhere { SessionsTable.lastUsedAt less cutoff }
        }
    }
}
