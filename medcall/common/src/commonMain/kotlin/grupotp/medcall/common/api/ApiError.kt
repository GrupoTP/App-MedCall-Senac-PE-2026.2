package grupotp.medcall.common.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val code: ErrorCode,
    val message: String,
)
