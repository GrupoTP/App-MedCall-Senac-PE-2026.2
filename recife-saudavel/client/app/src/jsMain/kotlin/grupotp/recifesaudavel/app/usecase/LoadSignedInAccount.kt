package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

class LoadSignedInAccount(
    private val accounts: LocalAccountGateway,
) {
    suspend operator fun invoke(): AuthenticatedAccount? = accounts.load()
}
