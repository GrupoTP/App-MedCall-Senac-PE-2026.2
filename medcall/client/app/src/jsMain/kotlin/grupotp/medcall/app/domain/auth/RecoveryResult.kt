package grupotp.medcall.app.domain.auth

sealed interface RecoveryResult {
    data object Accepted : RecoveryResult

    data class Failed(val error: AuthError) : RecoveryResult
}
