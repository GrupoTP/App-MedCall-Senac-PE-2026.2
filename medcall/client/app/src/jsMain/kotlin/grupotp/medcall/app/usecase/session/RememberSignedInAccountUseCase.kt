package grupotp.medcall.app.usecase.session

import grupotp.medcall.app.gateway.local.LocalAccountGateway
import grupotp.medcall.common.auth.AuthenticatedAccount

class RememberSignedInAccountUseCase(
    private val accounts: LocalAccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount) {
        accounts.save(account)
    }
}
