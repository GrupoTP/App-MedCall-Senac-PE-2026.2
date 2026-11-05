package grupotp.recifesaudavel.server.database.entity

import grupotp.recifesaudavel.server.database.table.UsersTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(UsersTable)

    var fullName by UsersTable.fullName
    var socialName by UsersTable.socialName
    var email by UsersTable.email
    var passwordHash by UsersTable.passwordHash
    var primaryPhone by UsersTable.primaryPhone
    var primaryPhoneWhatsapp by UsersTable.primaryPhoneWhatsapp
    var secondaryPhone by UsersTable.secondaryPhone
    var secondaryPhoneWhatsapp by UsersTable.secondaryPhoneWhatsapp
    var emergencyContactName by UsersTable.emergencyContactName
    var emergencyContactPhone by UsersTable.emergencyContactPhone
    var acceptedPrivacyPolicy by UsersTable.acceptedPrivacyPolicy
    var acceptedTreatment by UsersTable.acceptedTreatment
    var wantsNotifications by UsersTable.wantsNotifications
    var createdAt by UsersTable.createdAt
    var lastLoginAt by UsersTable.lastLoginAt
    var deletionRequestedAt by UsersTable.deletionRequestedAt
}
