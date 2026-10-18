package grupotp.recifesaudavel.app.usecase.account

import grupotp.recifesaudavel.app.domain.account.AccountGateway
import grupotp.recifesaudavel.app.domain.scheduling.SchedulingResult
import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

class LoadAccountProfileUseCase(
    private val gateway: AccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount): SchedulingResult<AccountProfile> =
        gateway.profile(account.sessionToken)
}
