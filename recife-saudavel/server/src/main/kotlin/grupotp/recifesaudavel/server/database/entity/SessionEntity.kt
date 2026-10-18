package grupotp.recifesaudavel.server.database.entity

import grupotp.recifesaudavel.server.database.table.SessionsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/** [userId] reads the column without loading the [user], which every request would pay for. */
class SessionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SessionEntity>(SessionsTable)

    var user by UserEntity referencedOn SessionsTable.user
    val userId by SessionsTable.user
    var createdAt by SessionsTable.createdAt
    var lastUsedAt by SessionsTable.lastUsedAt
}
