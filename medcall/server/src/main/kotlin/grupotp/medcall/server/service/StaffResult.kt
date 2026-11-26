package grupotp.medcall.server.service

import grupotp.medcall.server.domain.ServiceError

sealed interface StaffResult<out T> {
    data class Done<T>(val value: T) : StaffResult<T>
    data class Rejected(val error: ServiceError) : StaffResult<Nothing>
    data object NotFound : StaffResult<Nothing>
}

