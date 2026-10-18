package grupotp.recifesaudavel.app.usecase.session

import grupotp.recifesaudavel.app.domain.session.LocalAccountGateway
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

class LoadSignedInAccountUseCase(
    private val accounts: LocalAccountGateway,
) {
    suspend operator fun invoke(): AuthenticatedAccount? = accounts.load()
}
