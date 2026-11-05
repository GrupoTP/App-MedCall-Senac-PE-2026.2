package grupotp.medcall.app.fake

import grupotp.medcall.app.gateway.local.PendingSignOutGateway
import grupotp.medcall.common.security.SecretString

internal class MemoryPendingSignOuts : PendingSignOutGateway {
    var tokens: List<SecretString> = emptyList()

    override suspend fun loadPendingSignOuts(): List<SecretString> = tokens

    override suspend fun savePendingSignOuts(tokens: List<SecretString>) {
        this.tokens = tokens
    }
}
