package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.security.SecretString

/**
 * Settings API of the server. Every call needs the session token and the network.
 */
interface AccountGateway {
    suspend fun profile(token: SecretString): SchedulingResult<AccountProfile>

    suspend fun update(token: SecretString, request: UpdateProfileRequest): SchedulingResult<AccountProfile>

    suspend fun updateConsents(token: SecretString, request: UpdateConsentsRequest): SchedulingResult<AccountProfile>

    suspend fun changePassword(token: SecretString, request: ChangePasswordRequest): SchedulingResult<AccountProfile>

    suspend fun requestDeletion(token: SecretString): SchedulingResult<AccountProfile>

    /** Asks the server to forget [token]. */
    suspend fun signOut(token: SecretString): SchedulingResult<Unit>
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
    /** @param signOutOtherSessions also end the account's sessions on other devices; this one stays. */
    suspend operator fun invoke(
        account: AuthenticatedAccount,
        currentPassword: SecretString,
        newPassword: SecretString,
        signOutOtherSessions: Boolean,
    ): SchedulingResult<AccountProfile> =
        gateway.changePassword(
            account.sessionToken,
            ChangePasswordRequest(currentPassword, newPassword, signOutOtherSessions),
        )
}

/** The server also ends every session of the account, this one included. */
class RequestAccountDeletion(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): SchedulingResult<AccountProfile> =
        gateway.requestDeletion(account.sessionToken)
}
