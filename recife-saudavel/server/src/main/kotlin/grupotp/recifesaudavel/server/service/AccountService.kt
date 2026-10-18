package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.security.PasswordHasher
import java.time.Clock
import kotlin.time.toKotlinInstant

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
    private val sessions: SessionService,
    private val clock: Clock,
) {
    fun profile(userId: Int): AccountProfile? = users.findAccountProfile(userId)

    fun update(userId: Int, request: UpdateProfileRequest): AccountResult {
        validate(request)?.let { return AccountResult.Rejected(it) }
        if (users.emailTakenByOther(request.email.trim().lowercase(), userId)) {
            return AccountResult.Rejected(EMAIL_TAKEN)
        }
        if (users.findAccountProfile(userId) == null) {
            return AccountResult.UnknownUser
        }
        if (!users.updateProfile(userId, request)) {
            return AccountResult.Rejected(EMAIL_TAKEN)
        }
        return reloaded(userId)
    }

    fun updateNotifications(userId: Int, wants: Boolean): AccountResult {
        if (users.findAccountProfile(userId) == null) {
            return AccountResult.UnknownUser
        }
        users.updateWantsNotifications(userId, wants)
        return reloaded(userId)
    }

    /**
     * @param keepSessionId session making the request. With [ChangePasswordRequest.signOutOtherSessions]
     * every other session of the account ends, and this one stays.
     */
    fun changePassword(userId: Int, request: ChangePasswordRequest, keepSessionId: Int?): AccountResult {
        val stored = users.findPasswordHash(userId) ?: return AccountResult.UnknownUser
        return when {
            !passwords.verify(request.currentPassword, stored) -> AccountResult.Rejected("Senha atual incorreta.")
            request.newPassword.value.length < MIN_PASSWORD -> AccountResult.Rejected("Mínimo 6 caracteres.")
            request.newPassword == request.currentPassword ->
                AccountResult.Rejected("A nova senha precisa ser diferente da atual.")
            else -> {
                users.updatePasswordHash(userId, passwords.hash(request.newPassword))
                if (request.signOutOtherSessions) {
                    sessions.revokeAll(userId, keepId = keepSessionId)
                }
                reloaded(userId)
            }
        }
    }

    /**
     * Records that the person asked to delete the account and ends every session of it, this one
     * included. Nothing is removed here: the deletion follows the legal retention period and is
     * handled by the clinic. Asking twice keeps the first date.
     */
    fun requestDeletion(userId: Int): AccountResult {
        val current = users.findAccountProfile(userId) ?: return AccountResult.UnknownUser
        if (current.deletionRequestedAt == null) {
            users.recordDeletionRequest(userId, clock.instant().toKotlinInstant())
        }
        val result = reloaded(userId)
        sessions.revokeAll(userId)
        return result
    }

    private fun reloaded(userId: Int): AccountResult =
        users.findAccountProfile(userId)?.let { AccountResult.Done(it) } ?: AccountResult.UnknownUser

    private fun validate(request: UpdateProfileRequest): String? = when {
        request.fullName.isBlank() -> "Nome completo é obrigatório."
        !request.email.contains("@") -> "Informe um e-mail válido."
        request.primaryPhone.isEmpty() -> "Telefone principal é obrigatório."
        !request.primaryPhone.hasValidLength || !request.secondaryPhone.hasValidLength ||
            !request.emergencyContactPhone.hasValidLength -> "Telefone inválido."
        else -> null
    }

    private companion object {
        const val MIN_PASSWORD = 6
        const val EMAIL_TAKEN = "E-mail já cadastrado."
    }
}
