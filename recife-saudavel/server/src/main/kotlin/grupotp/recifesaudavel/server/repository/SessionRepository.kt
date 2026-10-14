package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.server.database.SessionsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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

    fun findUserId(tokenHash: String): String? = transaction(database) {
        SessionsTable.selectAll()
            .where { SessionsTable.tokenHash eq tokenHash }
            .singleOrNull()
            ?.get(SessionsTable.userId)
    }
}
