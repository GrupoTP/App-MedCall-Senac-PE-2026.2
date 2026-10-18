package grupotp.recifesaudavel.app.fake

import grupotp.recifesaudavel.app.domain.session.LocalAccountGateway
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount

internal class MemoryLocalAccounts(var stored: AuthenticatedAccount? = null) : LocalAccountGateway {
    override suspend fun save(account: AuthenticatedAccount) {
        stored = account
    }

    override suspend fun load(): AuthenticatedAccount? = stored

    override suspend fun clear() {
        stored = null
    }
}
