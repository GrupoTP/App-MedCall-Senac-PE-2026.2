package grupotp.recifesaudavel.common.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val message: String,
)
