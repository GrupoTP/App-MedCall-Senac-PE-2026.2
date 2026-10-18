package grupotp.recifesaudavel.app.fake

import grupotp.recifesaudavel.app.domain.session.PendingSignOutGateway
import grupotp.recifesaudavel.common.security.SecretString

internal class MemoryPendingSignOuts : PendingSignOutGateway {
    var tokens: List<SecretString> = emptyList()

    override suspend fun loadPendingSignOuts(): List<SecretString> = tokens

    override suspend fun savePendingSignOuts(tokens: List<SecretString>) {
        this.tokens = tokens
    }
}
