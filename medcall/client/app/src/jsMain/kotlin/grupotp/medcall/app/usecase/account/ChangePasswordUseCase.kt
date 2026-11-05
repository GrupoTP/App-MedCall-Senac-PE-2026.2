package grupotp.medcall.app.usecase.account

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.gateway.account.AccountGateway
import grupotp.medcall.common.account.AccountProfile
import grupotp.medcall.common.account.ChangePasswordRequest
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.security.SecretString

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
