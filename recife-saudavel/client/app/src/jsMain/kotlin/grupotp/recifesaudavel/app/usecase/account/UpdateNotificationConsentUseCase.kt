package grupotp.recifesaudavel.app.usecase.account

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.account.AccountGateway
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.account.UpdateConsentsRequest
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

class UpdateNotificationConsentUseCase(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount, wants: Boolean): RemoteResult<AccountProfile> =
        gateway.updateConsents(account.sessionToken, UpdateConsentsRequest(wantsNotifications = wants))
}
