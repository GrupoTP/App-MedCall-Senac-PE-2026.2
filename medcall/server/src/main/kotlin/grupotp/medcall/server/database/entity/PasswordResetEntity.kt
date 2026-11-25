package grupotp.medcall.server.database.entity

import grupotp.medcall.server.database.table.PasswordResetsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class PasswordResetEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PasswordResetEntity>(PasswordResetsTable)

    var user by UserEntity referencedOn PasswordResetsTable.user
    var codeHash by PasswordResetsTable.codeHash
    var expiresAt by PasswordResetsTable.expiresAt
    var usedAt by PasswordResetsTable.usedAt
}
