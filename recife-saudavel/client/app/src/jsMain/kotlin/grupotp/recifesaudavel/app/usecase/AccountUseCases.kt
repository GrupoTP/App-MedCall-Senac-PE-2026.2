package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.AccountProfile
import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.ChangePasswordRequest
import grupotp.recifesaudavel.common.UpdateConsentsRequest
import grupotp.recifesaudavel.common.UpdateProfileRequest

/**
 * Settings API of the server. Every call needs the session token and the network.
 */
interface AccountGateway {
    suspend fun profile(token: String): SchedulingResult<AccountProfile>

    suspend fun update(token: String, request: UpdateProfileRequest): SchedulingResult<AccountProfile>

    suspend fun updateConsents(token: String, request: UpdateConsentsRequest): SchedulingResult<AccountProfile>

    suspend fun changePassword(token: String, request: ChangePasswordRequest): SchedulingResult<AccountProfile>

    suspend fun requestDeletion(token: String): SchedulingResult<AccountProfile>

    /** Asks the server to forget [token]. */
    suspend fun signOut(token: String): SchedulingResult<Unit>
}

class LoadAccountProfile(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): SchedulingResult<AccountProfile> =
        gateway.profile(account.sessionToken)
}

/**
 * @property account the signed-in profile the device keeps, already updated.
 */
data class SavedProfile(
    val profile: AccountProfile,
    val account: AuthenticatedAccount,
)

/**
 * Sends the edited profile. The copy on the device only changes after the server accepted it,
 * so a refused or offline save never leaves the device ahead of the server.
 */
class SaveAccountProfile(
    private val gateway: AccountGateway,
    private val accounts: LocalAccountGateway,
) {
    suspend operator fun invoke(
        account: AuthenticatedAccount,
        request: UpdateProfileRequest,
    ): SchedulingResult<SavedProfile> {
        return when (val result = gateway.update(account.sessionToken, request)) {
            is SchedulingResult.Failed -> result
            is SchedulingResult.Success -> {
                val profile = result.value
                val updated = account.copy(
                    fullName = profile.fullName,
                    socialName = profile.socialName,
                    email = profile.email,
                    phone = profile.primaryPhone,
                )
                runCatching { accounts.save(updated) }
                SchedulingResult.Success(SavedProfile(profile, updated))
            }
        }
    }
}

class UpdateNotificationConsent(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount, wants: Boolean): SchedulingResult<AccountProfile> =
        gateway.updateConsents(account.sessionToken, UpdateConsentsRequest(wantsNotifications = wants))
}

class ChangePassword(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(
        account: AuthenticatedAccount,
        currentPassword: String,
        newPassword: String,
    ): SchedulingResult<AccountProfile> =
        gateway.changePassword(account.sessionToken, ChangePasswordRequest(currentPassword, newPassword))
}

class RequestAccountDeletion(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): SchedulingResult<AccountProfile> =
        gateway.requestDeletion(account.sessionToken)
}
