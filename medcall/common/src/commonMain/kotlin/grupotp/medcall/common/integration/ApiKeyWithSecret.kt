package grupotp.medcall.common.integration

import grupotp.medcall.common.security.SecretString
import kotlinx.serialization.Serializable

/** [key] is shown once; the server keeps only its hash. */
@Serializable
data class ApiKeyWithSecret(
    val apiKey: ApiKey,
    val key: SecretString,
)

