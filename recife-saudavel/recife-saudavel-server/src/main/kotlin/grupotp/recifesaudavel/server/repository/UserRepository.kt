package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.RegisterAccountRequest
import grupotp.recifesaudavel.server.database.UsersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class StoredUser(
    val account: AuthenticatedAccount,
    val passwordHash: String,
)

class UserRepository(
    private val database: Database,
) {
    fun findByEmail(email: String): StoredUser? = transaction(database) {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.toStoredUser()
    }

    fun insert(id: String, request: RegisterAccountRequest, passwordHash: String): AuthenticatedAccount =
        transaction(database) {
            UsersTable.insert {
                it[UsersTable.id] = id
                it[fullName] = request.fullName.trim()
                it[socialName] = request.socialName.trim()
                it[email] = request.email.trim().lowercase()
                it[UsersTable.passwordHash] = passwordHash
                it[primaryPhone] = request.primaryPhone.trim()
                it[primaryPhoneWhatsapp] = request.primaryPhoneWhatsapp
                it[secondaryPhone] = request.secondaryPhone.trim()
                it[secondaryPhoneWhatsapp] = request.secondaryPhoneWhatsapp
                it[emergencyContactName] = request.emergencyContactName.trim()
                it[emergencyContactPhone] = request.emergencyContactPhone.trim()
                it[acceptedPrivacyPolicy] = request.acceptedPrivacyPolicy
                it[acceptedTreatment] = request.acceptedTreatment
                it[wantsNotifications] = request.wantsNotifications
            }
            AuthenticatedAccount(
                id = id,
                fullName = request.fullName.trim(),
                socialName = request.socialName.trim(),
                email = request.email.trim().lowercase(),
            )
        }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toStoredUser(): StoredUser = StoredUser(
        account = AuthenticatedAccount(
            id = this[UsersTable.id],
            fullName = this[UsersTable.fullName],
            socialName = this[UsersTable.socialName],
            email = this[UsersTable.email],
        ),
        passwordHash = this[UsersTable.passwordHash],
    )
}
