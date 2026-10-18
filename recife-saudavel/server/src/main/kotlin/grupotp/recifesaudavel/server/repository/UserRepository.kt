package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.phone.Phone
import grupotp.recifesaudavel.server.database.UsersTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

data class StoredUser(
    val account: AuthenticatedAccount,
    val passwordHash: String,
)

data class UserProfile(
    val fullName: String,
    val socialName: String,
    val primaryPhone: String,
    val primaryPhoneWhatsapp: Boolean,
    val secondaryPhone: String,
    val secondaryPhoneWhatsapp: Boolean,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
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

    fun findById(id: String): AuthenticatedAccount? = transaction(database) {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toStoredUser()
            ?.account
    }

    /** Contact data typed at registration, as it is now. */
    fun findProfile(id: String): UserProfile? = transaction(database) {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.let {
                UserProfile(
                    fullName = it[UsersTable.fullName],
                    socialName = it[UsersTable.socialName],
                    primaryPhone = it[UsersTable.primaryPhone],
                    primaryPhoneWhatsapp = it[UsersTable.primaryPhoneWhatsapp],
                    secondaryPhone = it[UsersTable.secondaryPhone],
                    secondaryPhoneWhatsapp = it[UsersTable.secondaryPhoneWhatsapp],
                    emergencyContactName = it[UsersTable.emergencyContactName],
                    emergencyContactPhone = it[UsersTable.emergencyContactPhone],
                )
            }
    }

    /** Everything the settings screen shows. */
    fun findAccountProfile(id: String): AccountProfile? = transaction(database) {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.let {
                AccountProfile(
                    fullName = it[UsersTable.fullName],
                    socialName = it[UsersTable.socialName],
                    email = it[UsersTable.email],
                    primaryPhone = it[UsersTable.primaryPhone],
                    primaryPhoneWhatsapp = it[UsersTable.primaryPhoneWhatsapp],
                    secondaryPhone = it[UsersTable.secondaryPhone],
                    secondaryPhoneWhatsapp = it[UsersTable.secondaryPhoneWhatsapp],
                    emergencyContactName = it[UsersTable.emergencyContactName],
                    emergencyContactPhone = it[UsersTable.emergencyContactPhone],
                    acceptedPrivacyPolicy = it[UsersTable.acceptedPrivacyPolicy],
                    acceptedTreatment = it[UsersTable.acceptedTreatment],
                    wantsNotifications = it[UsersTable.wantsNotifications],
                    createdAt = it[UsersTable.createdAt],
                    lastLoginAt = it[UsersTable.lastLoginAt],
                    deletionRequestedAt = it[UsersTable.deletionRequestedAt],
                )
            }
    }

    /** True when another account already uses [email] (already trimmed and lower case). */
    fun emailTakenByOther(email: String, userId: String): Boolean = transaction(database) {
        !UsersTable.selectAll()
            .where { (UsersTable.email eq email) and (UsersTable.id neq userId) }
            .empty()
    }

    /**
     * Returns false when the new e-mail belongs to another account. [emailTakenByOther] catches
     * that first, but two requests at once can both pass it: the unique index decides.
     */
    fun updateProfile(id: String, request: UpdateProfileRequest): Boolean = try {
        transaction(database) {
            UsersTable.update({ UsersTable.id eq id }) {
                it[fullName] = request.fullName.trim()
                it[socialName] = request.socialName.trim()
                it[email] = request.email.trim().lowercase()
                it[primaryPhone] = Phone.digits(request.primaryPhone)
                it[primaryPhoneWhatsapp] = request.primaryPhoneWhatsapp
                it[secondaryPhone] = Phone.digits(request.secondaryPhone)
                it[secondaryPhoneWhatsapp] = request.secondaryPhoneWhatsapp
                it[emergencyContactName] = request.emergencyContactName.trim()
                it[emergencyContactPhone] = Phone.digits(request.emergencyContactPhone)
            }
        }
        true
    } catch (error: ExposedSQLException) {
        if (!error.isUniqueViolation()) {
            throw error
        }
        false
    }

    fun updateWantsNotifications(id: String, wants: Boolean) {
        transaction(database) {
            UsersTable.update({ UsersTable.id eq id }) { it[wantsNotifications] = wants }
        }
    }

    fun findPasswordHash(id: String): String? = transaction(database) {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.get(UsersTable.passwordHash)
    }

    fun updatePasswordHash(id: String, passwordHash: String) {
        transaction(database) {
            UsersTable.update({ UsersTable.id eq id }) { it[UsersTable.passwordHash] = passwordHash }
        }
    }

    fun recordLogin(id: String, at: Long) {
        transaction(database) {
            UsersTable.update({ UsersTable.id eq id }) { it[lastLoginAt] = at }
        }
    }

    fun recordDeletionRequest(id: String, at: Long) {
        transaction(database) {
            UsersTable.update({ UsersTable.id eq id }) { it[deletionRequestedAt] = at }
        }
    }

    /**
     * Returns null when the e-mail is already registered. Checking first is not enough: two
     * sign-ups at once can both find the e-mail free, so the unique index is what decides.
     */
    fun insert(
        id: String,
        request: RegisterAccountRequest,
        passwordHash: String,
        now: Long = System.currentTimeMillis(),
    ): AuthenticatedAccount? = try {
        transaction(database) {
            UsersTable.insert {
                it[UsersTable.id] = id
                it[fullName] = request.fullName.trim()
                it[socialName] = request.socialName.trim()
                it[email] = request.email.trim().lowercase()
                it[UsersTable.passwordHash] = passwordHash
                it[primaryPhone] = Phone.digits(request.primaryPhone)
                it[primaryPhoneWhatsapp] = request.primaryPhoneWhatsapp
                it[secondaryPhone] = Phone.digits(request.secondaryPhone)
                it[secondaryPhoneWhatsapp] = request.secondaryPhoneWhatsapp
                it[emergencyContactName] = request.emergencyContactName.trim()
                it[emergencyContactPhone] = Phone.digits(request.emergencyContactPhone)
                it[acceptedPrivacyPolicy] = request.acceptedPrivacyPolicy
                it[acceptedTreatment] = request.acceptedTreatment
                it[wantsNotifications] = request.wantsNotifications
                it[createdAt] = now
                it[lastLoginAt] = now
            }
            AuthenticatedAccount(
                id = id,
                fullName = request.fullName.trim(),
                socialName = request.socialName.trim(),
                email = request.email.trim().lowercase(),
                phone = Phone.digits(request.primaryPhone),
            )
        }
    } catch (error: ExposedSQLException) {
        if (!error.isUniqueViolation()) {
            throw error
        }
        null
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toStoredUser(): StoredUser = StoredUser(
        account = AuthenticatedAccount(
            id = this[UsersTable.id],
            fullName = this[UsersTable.fullName],
            socialName = this[UsersTable.socialName],
            email = this[UsersTable.email],
            phone = this[UsersTable.primaryPhone],
        ),
        passwordHash = this[UsersTable.passwordHash],
    )
}
