package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.AuthenticatedAccount

class RememberSignedInAccount(
    private val accounts: LocalAccountGateway,
) {
    suspend operator fun invoke(account: AuthenticatedAccount) {
        accounts.save(account)
    }
}
