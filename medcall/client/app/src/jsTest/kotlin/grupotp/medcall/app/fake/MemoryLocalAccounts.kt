package grupotp.medcall.app.fake

import grupotp.medcall.app.gateway.local.LocalAccountGateway
import grupotp.medcall.common.auth.AuthenticatedAccount

internal class MemoryLocalAccounts(var stored: AuthenticatedAccount? = null) : LocalAccountGateway {
    override suspend fun save(account: AuthenticatedAccount) {
        stored = account
    }

    override suspend fun load(): AuthenticatedAccount? = stored

    override suspend fun clear() {
        stored = null
    }
}
