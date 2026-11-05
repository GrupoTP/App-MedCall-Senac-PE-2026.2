package grupotp.medcall.app.usecase.account

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.domain.account.SavedProfile
import grupotp.medcall.app.gateway.account.AccountGateway
import grupotp.medcall.app.gateway.local.LocalAccountGateway
import grupotp.medcall.common.account.UpdateProfileRequest
import grupotp.medcall.common.auth.AuthenticatedAccount

/**
 * Sends the edited profile. The copy on the device only changes after the server accepted it,
 * so a refused or offline save never leaves the device ahead of the server.
 */
class SaveAccountProfileUseCase(
    private val gateway: AccountGateway,
    private val accounts: LocalAccountGateway,
) {
    suspend operator fun invoke(
        account: AuthenticatedAccount,
        request: UpdateProfileRequest,
    ): RemoteResult<SavedProfile> {
        return when (val result = gateway.update(account.sessionToken, request)) {
            is RemoteResult.Failed -> result
            is RemoteResult.Success -> {
                val profile = result.value
                val updated = account.copy(
                    fullName = profile.fullName,
                    socialName = profile.socialName,
                    email = profile.email,
                    phone = profile.primaryPhone,
                )
                runCatching { accounts.save(updated) }
                RemoteResult.Success(SavedProfile(profile, updated))
            }
        }
    }
}
