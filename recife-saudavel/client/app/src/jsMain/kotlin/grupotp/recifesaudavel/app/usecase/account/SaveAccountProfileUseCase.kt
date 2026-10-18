package grupotp.recifesaudavel.app.usecase.account

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.account.AccountGateway
import grupotp.recifesaudavel.app.domain.account.SavedProfile
import grupotp.recifesaudavel.app.domain.session.LocalAccountGateway
import grupotp.recifesaudavel.common.account.UpdateProfileRequest
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

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
