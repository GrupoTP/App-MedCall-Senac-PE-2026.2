package grupotp.medcall.common.integration

import kotlinx.serialization.Serializable

@Serializable
data class CreateApiKeyRequest(
    val name: String,
)

