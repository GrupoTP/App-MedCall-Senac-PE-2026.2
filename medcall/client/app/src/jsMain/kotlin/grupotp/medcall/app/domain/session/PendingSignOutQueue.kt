package grupotp.recifesaudavel.app.domain.session

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.domain.RemoteResult
import grupotp.recifesaudavel.app.gateway.account.AccountGateway
import grupotp.recifesaudavel.app.gateway.local.PendingSignOutGateway
import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Revokes sessions on the server. A token is only queued when the server was unreachable:
 * a refusal means the session is already gone, so there is nothing left to retry.
 */
class PendingSignOutQueue(
    private val remote: AccountGateway,
    private val storage: PendingSignOutGateway,
) {
    private val lock = Mutex()

    /** Asks the server now; on [RemoteError.Offline] the token is queued for [flush]. */
    suspend fun revokeOrQueue(token: SecretString) {
        if (!isUnreachable(runCatching { remote.signOut(token) }.getOrNull())) {
            return
        }
        lock.withLock {
            val queued = runCatching { storage.loadPendingSignOuts() }.getOrDefault(emptyList())
            if (token !in queued) {
                storage.savePendingSignOuts(queued + token)
            }
        }
    }

    /**
     * Retries every queued token. Stops at the first one that is still unreachable, since the
     * others would fail the same way.
     */
    suspend fun flush() {
        lock.withLock {
            val queued = runCatching { storage.loadPendingSignOuts() }.getOrDefault(emptyList())
            if (queued.isEmpty()) {
                return
            }
            val remaining = queued.toMutableList()
            for (token in queued) {
                if (isUnreachable(runCatching { remote.signOut(token) }.getOrNull())) {
                    break
                }
                remaining.remove(token)
            }
            if (remaining.size != queued.size) {
                runCatching { storage.savePendingSignOuts(remaining) }
            }
        }
    }

    /** A null [result] is a call that threw, which also means the server was not reached. */
    private fun isUnreachable(result: RemoteResult<Unit>?): Boolean =
        result == null || (result is RemoteResult.Failed && result.error == RemoteError.Offline)
}
