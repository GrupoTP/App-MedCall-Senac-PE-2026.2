package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.AuthenticatedAccount

class LoadSignedInAccount(
    private val accounts: LocalAccountGateway,
) {
    suspend operator fun invoke(): AuthenticatedAccount? = accounts.load()
}
