package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.account.AccountProfile

sealed interface AccountResult {
    data class Done(val profile: AccountProfile) : AccountResult
    data class Rejected(val error: ServiceError) : AccountResult
    data object UnknownUser : AccountResult
}
