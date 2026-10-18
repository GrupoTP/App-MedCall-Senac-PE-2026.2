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

data class StoredSession(
    val userId: String,
    val createdAt: Long,
)

class SessionRepository(
    private val database: Database,
) {
    fun insert(tokenHash: String, userId: String, createdAt: Long) {
        transaction(database) {
            SessionsTable.insert {
                it[SessionsTable.tokenHash] = tokenHash
                it[SessionsTable.userId] = userId
                it[SessionsTable.createdAt] = createdAt
            }
        }
    }

    fun delete(tokenHash: String) {
        transaction(database) {
            SessionsTable.deleteWhere { SessionsTable.tokenHash eq tokenHash }
        }
    }

    /** Deletes every session of [userId] except the one with [keepTokenHash], if given. */
    fun deleteAllOf(userId: String, keepTokenHash: String? = null) {
        transaction(database) {
            SessionsTable.deleteWhere {
                if (keepTokenHash == null) {
                    SessionsTable.userId eq userId
                } else {
                    (SessionsTable.userId eq userId) and (SessionsTable.tokenHash neq keepTokenHash)
                }
            }
        }
    }

    fun deleteCreatedBefore(cutoff: Long) {
        transaction(database) {
            SessionsTable.deleteWhere { SessionsTable.createdAt less cutoff }
        }
    }

    fun find(tokenHash: String): StoredSession? = transaction(database) {
        SessionsTable.selectAll()
            .where { SessionsTable.tokenHash eq tokenHash }
            .singleOrNull()
            ?.let { StoredSession(userId = it[SessionsTable.userId], createdAt = it[SessionsTable.createdAt]) }
    }
}
