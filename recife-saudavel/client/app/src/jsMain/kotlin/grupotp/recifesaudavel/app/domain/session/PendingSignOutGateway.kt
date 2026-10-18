package grupotp.recifesaudavel.app.domain.session

import grupotp.recifesaudavel.common.security.SecretString

/**
 * Session tokens whose sign-out could not reach the server, kept on the device until they can.
 * The only data that survives [LocalAccountGateway.clear], which wipes everything else.
 */
interface PendingSignOutGateway {
    suspend fun loadPendingSignOuts(): List<SecretString>

    suspend fun savePendingSignOuts(tokens: List<SecretString>)
}
