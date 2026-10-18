package grupotp.recifesaudavel.app.usecase

import grupotp.recifesaudavel.common.security.SecretString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Session tokens whose sign-out could not reach the server, kept on the device until they can.
 * The only data that survives [LocalAccountGateway.clear], which wipes everything else.
 */
interface PendingSignOutGateway {
    suspend fun loadPendingSignOuts(): List<SecretString>

    suspend fun savePendingSignOuts(tokens: List<SecretString>)
}

/**
 * Revokes sessions on the server. A token is only queued when the server was unreachable:
 * a refusal means the session is already gone, so there is nothing left to retry.
 */
class PendingSignOuts(
    private val remote: AccountGateway,
    private val storage: PendingSignOutGateway,
) {
    private val lock = Mutex()

    /** Asks the server now; on [SchedulingError.Offline] the token is queued for [flush]. */
    suspend fun revokeOrQueue(token: SecretString) {
        val result = runCatching { remote.signOut(token) }.getOrNull()
        val unreachable = result == null || (result is SchedulingResult.Failed && result.error == SchedulingError.Offline)
        if (unreachable) {
            lock.withLock {
                val queued = runCatching { storage.loadPendingSignOuts() }.getOrDefault(emptyList())
                if (token !in queued) {
                    storage.savePendingSignOuts(queued + token)
                }
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
                val result = runCatching { remote.signOut(token) }.getOrNull()
                if (result == null || (result is SchedulingResult.Failed && result.error == SchedulingError.Offline)) {
                    break
                }
                remaining.remove(token)
            }
            if (remaining.size != queued.size) {
                runCatching { storage.savePendingSignOuts(remaining) }
            }
        }
    }
}
