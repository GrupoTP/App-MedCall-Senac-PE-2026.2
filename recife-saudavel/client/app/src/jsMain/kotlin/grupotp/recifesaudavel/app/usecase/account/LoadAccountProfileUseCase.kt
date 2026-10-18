package grupotp.recifesaudavel.app.usecase.account

import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.domain.account.AccountGateway
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

class LoadAccountProfileUseCase(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): RemoteResult<AccountProfile> =
        gateway.profile(account.sessionToken)
}
