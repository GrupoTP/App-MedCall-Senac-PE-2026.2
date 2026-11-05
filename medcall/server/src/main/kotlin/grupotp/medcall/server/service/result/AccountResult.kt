package grupotp.medcall.server.service.result

import grupotp.medcall.common.account.AccountProfile
import grupotp.medcall.server.domain.ServiceError

sealed interface AccountResult {
    data class Done(val profile: AccountProfile) : AccountResult
    data class Rejected(val error: ServiceError) : AccountResult
    data object UnknownUser : AccountResult
}
