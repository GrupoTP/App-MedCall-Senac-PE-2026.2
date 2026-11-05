package grupotp.medcall.app.usecase.account

import grupotp.medcall.app.domain.RemoteResult
import grupotp.medcall.app.gateway.account.AccountGateway
import grupotp.medcall.common.account.AccountProfile
import grupotp.medcall.common.auth.AuthenticatedAccount

/** The server also ends every session of the account, this one included. */
class RequestAccountDeletionUseCase(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): RemoteResult<AccountProfile> =
        gateway.requestDeletion(account.sessionToken)
}
