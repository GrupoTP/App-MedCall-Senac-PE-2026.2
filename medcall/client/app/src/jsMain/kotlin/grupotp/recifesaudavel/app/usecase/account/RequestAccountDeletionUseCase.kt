package grupotp.recifesaudavel.app.usecase.account

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.account.AccountGateway
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

/** The server also ends every session of the account, this one included. */
class RequestAccountDeletionUseCase(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): RemoteResult<AccountProfile> =
        gateway.requestDeletion(account.sessionToken)
}
