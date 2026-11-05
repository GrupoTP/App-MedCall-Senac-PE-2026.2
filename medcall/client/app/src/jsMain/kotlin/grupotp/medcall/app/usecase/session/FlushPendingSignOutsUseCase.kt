package grupotp.recifesaudavel.app.usecase.session

import grupotp.recifesaudavel.app.domain.session.PendingSignOutQueue

/**
 * Sends the sign-outs made offline, now that the server may be reachable again.
 */
class FlushPendingSignOutsUseCase(
    private val pendingSignOuts: PendingSignOutQueue,
) {
    suspend operator fun invoke() {
        pendingSignOuts.flush()
    }
}
