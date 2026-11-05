package grupotp.medcall.app.usecase.account

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.gateway.account.AccountGateway
import grupotp.medcall.common.account.AccountProfile
import grupotp.medcall.common.account.UpdateConsentsRequest
import grupotp.medcall.common.auth.AuthenticatedAccount

class UpdateNotificationConsentUseCase(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount, wants: Boolean): RemoteResult<AccountProfile> =
        gateway.updateConsents(account.sessionToken, UpdateConsentsRequest(wantsNotifications = wants))
}
