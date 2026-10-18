package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.server.database.entity.UserEntity
import grupotp.recifesaudavel.server.database.table.UsersTable
import grupotp.recifesaudavel.server.domain.StoredUser
import grupotp.recifesaudavel.server.domain.UserProfile
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

class UserRepository(
    private val database: Database,
) {
    fun findByEmail(email: String): StoredUser? = transaction(database) {
        UserEntity.find { UsersTable.email eq email }
            .singleOrNull()
            ?.let { StoredUser(account = it.toAccount(), passwordHash = it.passwordHash) }
    }

    fun findById(id: Int): AuthenticatedAccount? = transaction(database) {
        UserEntity.findById(id)?.toAccount()
    }

    /** Contact data typed at registration, as it is now. */
    fun findProfile(id: Int): UserProfile? = transaction(database) {
        UserEntity.findById(id)?.let {
            UserProfile(
                fullName = it.fullName,
                socialName = it.socialName,
                primaryPhone = Phone(it.primaryPhone),
                primaryPhoneWhatsapp = it.primaryPhoneWhatsapp,
                secondaryPhone = Phone(it.secondaryPhone),
                secondaryPhoneWhatsapp = it.secondaryPhoneWhatsapp,
                emergencyContactName = it.emergencyContactName,
                emergencyContactPhone = Phone(it.emergencyContactPhone),
            )
        }
    }

    /** Everything the settings screen shows. */
    fun findAccountProfile(id: Int): AccountProfile? = transaction(database) {
        UserEntity.findById(id)?.let {
            AccountProfile(
                fullName = it.fullName,
                socialName = it.socialName,
                email = it.email,
                primaryPhone = Phone(it.primaryPhone),
                primaryPhoneWhatsapp = it.primaryPhoneWhatsapp,
                secondaryPhone = Phone(it.secondaryPhone),
                secondaryPhoneWhatsapp = it.secondaryPhoneWhatsapp,
                emergencyContactName = it.emergencyContactName,
                emergencyContactPhone = Phone(it.emergencyContactPhone),
                acceptedPrivacyPolicy = it.acceptedPrivacyPolicy,
                acceptedTreatment = it.acceptedTreatment,
                wantsNotifications = it.wantsNotifications,
                createdAt = it.createdAt,
                lastLoginAt = it.lastLoginAt,
                deletionRequestedAt = it.deletionRequestedAt,
            )
        }
    }

    /** True when another account already uses [email] (already trimmed and lower case). */
    fun emailTakenByOther(email: String, userId: Int): Boolean = transaction(database) {
        !UserEntity.find { (UsersTable.email eq email) and (UsersTable.id neq userId) }.empty()
    }

    /**
     * Returns false when the new e-mail belongs to another account. [emailTakenByOther] catches
     * that first, but two requests at once can both pass it: the unique index decides.
     */
    fun updateProfile(id: Int, request: UpdateProfileRequest): Boolean = try {
        transaction(database) {
            UserEntity.findById(id)?.apply {
                fullName = request.fullName.trim()
                socialName = request.socialName.trim()
                email = request.email.trim().lowercase()
                primaryPhone = request.primaryPhone.digits
                primaryPhoneWhatsapp = request.primaryPhoneWhatsapp
                secondaryPhone = request.secondaryPhone.digits
                secondaryPhoneWhatsapp = request.secondaryPhoneWhatsapp
                emergencyContactName = request.emergencyContactName.trim()
                emergencyContactPhone = request.emergencyContactPhone.digits
            }
        }
        true
    } catch (error: ExposedSQLException) {
        if (!error.isUniqueViolation()) {
            throw error
        }
        false
    }

    fun updateWantsNotifications(id: Int, wants: Boolean) {
        transaction(database) {
            UserEntity.findById(id)?.wantsNotifications = wants
        }
    }

    fun findPasswordHash(id: Int): String? = transaction(database) {
        UserEntity.findById(id)?.passwordHash
    }

    fun updatePasswordHash(id: Int, passwordHash: String) {
        transaction(database) {
            UserEntity.findById(id)?.passwordHash = passwordHash
        }
    }

    fun recordLogin(id: Int, at: Instant) {
        transaction(database) {
            UserEntity.findById(id)?.lastLoginAt = at
        }
    }

    fun recordDeletionRequest(id: Int, at: Instant) {
        transaction(database) {
            UserEntity.findById(id)?.deletionRequestedAt = at
        }
    }

    /**
     * Returns null when the e-mail is already registered. Checking first is not enough: two
     * sign-ups at once can both find the e-mail free, so the unique index is what decides.
     */
    fun insert(request: RegisterAccountRequest, passwordHash: String, now: Instant): AuthenticatedAccount? = try {
        transaction(database) {
            UserEntity.new {
                fullName = request.fullName.trim()
                socialName = request.socialName.trim()
                email = request.email.trim().lowercase()
                this.passwordHash = passwordHash
                primaryPhone = request.primaryPhone.digits
                primaryPhoneWhatsapp = request.primaryPhoneWhatsapp
                secondaryPhone = request.secondaryPhone.digits
                secondaryPhoneWhatsapp = request.secondaryPhoneWhatsapp
                emergencyContactName = request.emergencyContactName.trim()
                emergencyContactPhone = request.emergencyContactPhone.digits
                acceptedPrivacyPolicy = request.acceptedPrivacyPolicy
                acceptedTreatment = request.acceptedTreatment
                wantsNotifications = request.wantsNotifications
                createdAt = now
                lastLoginAt = now
            }.toAccount()
        }
    } catch (error: ExposedSQLException) {
        if (!error.isUniqueViolation()) {
            throw error
        }
        null
    }

    private fun UserEntity.toAccount(): AuthenticatedAccount = AuthenticatedAccount(
        id = id.value,
        fullName = fullName,
        socialName = socialName,
        email = email,
        phone = Phone(primaryPhone),
    )
}
