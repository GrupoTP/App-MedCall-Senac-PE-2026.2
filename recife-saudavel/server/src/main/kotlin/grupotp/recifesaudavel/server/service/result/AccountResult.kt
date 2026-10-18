package grupotp.recifesaudavel.server.service.result

import grupotp.recifesaudavel.common.account.AccountProfile
import grupotp.recifesaudavel.server.domain.ServiceError

sealed interface AccountResult {
    data class Done(val profile: AccountProfile) : AccountResult
    data class Rejected(val error: ServiceError) : AccountResult
    data object UnknownUser : AccountResult
}
