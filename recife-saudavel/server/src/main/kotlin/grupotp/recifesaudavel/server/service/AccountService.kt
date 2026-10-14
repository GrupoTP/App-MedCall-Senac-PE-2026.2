package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.AccountProfile
import grupotp.recifesaudavel.common.ChangePasswordRequest
import grupotp.recifesaudavel.common.UpdateProfileRequest
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.security.PasswordHasher
import java.time.Clock

sealed interface AccountResult {
    data class Done(val profile: AccountProfile) : AccountResult
    data class Rejected(val message: String) : AccountResult
    data object UnknownUser : AccountResult
}

/**
 * Settings of the signed-in person: profile, optional consent, password and the account deletion request.
 */
class AccountService(
    private val users: UserRepository,
    private val passwords: PasswordHasher,
    private val clock: Clock,
) {
    fun profile(userId: String): AccountProfile? = users.findAccountProfile(userId)

    fun update(userId: String, request: UpdateProfileRequest): AccountResult {
        validate(request)?.let { return AccountResult.Rejected(it) }
        if (users.emailTakenByOther(request.email.trim().lowercase(), userId)) {
            return AccountResult.Rejected("E-mail já cadastrado.")
        }
        if (users.findAccountProfile(userId) == null) {
            return AccountResult.UnknownUser
        }
        users.updateProfile(userId, request)
        return reloaded(userId)
    }

    fun updateNotifications(userId: String, wants: Boolean): AccountResult {
        if (users.findAccountProfile(userId) == null) {
            return AccountResult.UnknownUser
        }
        users.updateWantsNotifications(userId, wants)
        return reloaded(userId)
    }

    fun changePassword(userId: String, request: ChangePasswordRequest): AccountResult {
        val stored = users.findPasswordHash(userId) ?: return AccountResult.UnknownUser
        return when {
            !passwords.verify(request.currentPassword, stored) -> AccountResult.Rejected("Senha atual incorreta.")
            request.newPassword.length < MIN_PASSWORD -> AccountResult.Rejected("Mínimo 6 caracteres.")
            request.newPassword == request.currentPassword ->
                AccountResult.Rejected("A nova senha precisa ser diferente da atual.")
            else -> {
                users.updatePasswordHash(userId, passwords.hash(request.newPassword))
                reloaded(userId)
            }
        }
    }

    /**
     * Records that the person asked to delete the account. Nothing is removed here: the deletion
     * follows the legal retention period and is handled by the clinic. Asking twice keeps the first date.
     */
    fun requestDeletion(userId: String): AccountResult {
        val current = users.findAccountProfile(userId) ?: return AccountResult.UnknownUser
        if (current.deletionRequestedAt == null) {
            users.recordDeletionRequest(userId, clock.millis())
        }
        return reloaded(userId)
    }

    private fun reloaded(userId: String): AccountResult =
        users.findAccountProfile(userId)?.let { AccountResult.Done(it) } ?: AccountResult.UnknownUser

    private fun validate(request: UpdateProfileRequest): String? = when {
        request.fullName.isBlank() -> "Nome completo é obrigatório."
        !request.email.contains("@") -> "Informe um e-mail válido."
        request.primaryPhone.isBlank() -> "Telefone principal é obrigatório."
        else -> null
    }

    private companion object {
        const val MIN_PASSWORD = 6
    }
}
