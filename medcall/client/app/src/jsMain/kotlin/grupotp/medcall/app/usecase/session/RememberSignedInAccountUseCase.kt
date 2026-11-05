package grupotp.recifesaudavel.app.usecase.session

import grupotp.recifesaudavel.app.gateway.local.LocalAccountGateway
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

class RememberSignedInAccountUseCase(
    private val accounts: LocalAccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount) {
        accounts.save(account)
    }
}
