package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.api.ErrorCode
import grupotp.recifesaudavel.server.repository.UserRepository
import grupotp.recifesaudavel.server.security.PasswordHasher
import java.time.Clock
import kotlin.time.toKotlinInstant

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
            return AccountResult.Rejected(ServiceError(ErrorCode.EMAIL_TAKEN))
        }
        if (users.findAccountProfile(userId) == null) {
            return AccountResult.UnknownUser
        }
        if (!users.updateProfile(userId, request)) {
            return AccountResult.Rejected(ServiceError(ErrorCode.EMAIL_TAKEN))
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
            !passwords.verify(request.currentPassword, stored) ->
                AccountResult.Rejected(ServiceError(ErrorCode.CURRENT_PASSWORD_WRONG))
            request.newPassword.value.length < MIN_PASSWORD ->
                AccountResult.Rejected(ServiceError(ErrorCode.PASSWORD_TOO_SHORT, MIN_PASSWORD))
            request.newPassword == request.currentPassword ->
                AccountResult.Rejected(ServiceError(ErrorCode.PASSWORD_UNCHANGED))
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

    private fun validate(request: UpdateProfileRequest): ServiceError? = when {
        request.fullName.isBlank() -> ServiceError(ErrorCode.FULL_NAME_REQUIRED)
        !request.email.contains("@") -> ServiceError(ErrorCode.EMAIL_INVALID)
        request.primaryPhone.isEmpty() -> ServiceError(ErrorCode.PRIMARY_PHONE_REQUIRED)
        !request.primaryPhone.hasValidLength || !request.secondaryPhone.hasValidLength ||
            !request.emergencyContactPhone.hasValidLength -> ServiceError(ErrorCode.PHONE_INVALID)
        else -> null
    }

    private companion object {
        const val MIN_PASSWORD = 6
    }
}
