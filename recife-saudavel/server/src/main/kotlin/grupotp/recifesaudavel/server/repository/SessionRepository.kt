package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.server.database.SessionsTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/** @property lastUsedAt last recorded use, or the sign-in time when never recorded. */
data class StoredSession(
    val userId: String,
    val lastUsedAt: Long,
)

class SessionRepository(
    private val database: Database,
) {
    fun insert(id: String, userId: String, createdAt: Long) {
        transaction(database) {
            SessionsTable.insert {
                it[SessionsTable.id] = id
                it[SessionsTable.userId] = userId
                it[SessionsTable.createdAt] = createdAt
                it[lastUsedAt] = createdAt
            }
        }
    }

    fun delete(id: String) {
        transaction(database) {
            SessionsTable.deleteWhere { SessionsTable.id eq id }
        }
    }

    /** Deletes every session of [userId] except [keepId], if given. */
    fun deleteAllOf(userId: String, keepId: String? = null) {
        transaction(database) {
            SessionsTable.deleteWhere {
                if (keepId == null) {
                    SessionsTable.userId eq userId
                } else {
                    (SessionsTable.userId eq userId) and (SessionsTable.id neq keepId)
                }
            }
        }
    }

    fun touch(id: String, at: Long) {
        transaction(database) {
            SessionsTable.update({ SessionsTable.id eq id }) { it[lastUsedAt] = at }
        }
    }

    /** Deletes the sessions whose last use is before [cutoff]. */
    fun deleteUnusedSince(cutoff: Long) {
        transaction(database) {
            SessionsTable.deleteWhere { SessionsTable.lastUsedAt less cutoff }
        }
    }

    fun find(id: String): StoredSession? = transaction(database) {
        SessionsTable.selectAll()
            .where { SessionsTable.id eq id }
            .singleOrNull()
            ?.let { StoredSession(userId = it[SessionsTable.userId], lastUsedAt = it[SessionsTable.lastUsedAt]) }
    }
}
