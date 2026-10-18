package grupotp.recifesaudavel.app.usecase.account

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.account.AccountGateway
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.ChangePasswordRequest
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.security.SecretString

class ChangePasswordUseCase(
    private val gateway: AccountGateway,
) {
    /** @param signOutOtherSessions also end the account's sessions on other devices; this one stays. */
    suspend operator fun invoke(
        account: AuthenticatedAccount,
        currentPassword: SecretString,
        newPassword: SecretString,
        signOutOtherSessions: Boolean,
    ): RemoteResult<AccountProfile> =
        gateway.changePassword(
            account.sessionToken,
            ChangePasswordRequest(currentPassword, newPassword, signOutOtherSessions),
        )
}
