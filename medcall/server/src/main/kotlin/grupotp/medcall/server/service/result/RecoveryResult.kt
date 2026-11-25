package grupotp.medcall.server.service.result

import grupotp.medcall.server.domain.ServiceError

sealed interface RecoveryResult {
    data object Accepted : RecoveryResult

    /** The server cannot deliver a code, so it refuses every address the same way. */
    data object Unavailable : RecoveryResult

    data class Rejected(val error: ServiceError) : RecoveryResult
}
