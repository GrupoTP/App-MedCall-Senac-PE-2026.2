package grupotp.medcall.common.integration

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ApiKey(
    val id: Int,
    val name: String,
    val lastFour: String,
    val createdAt: Instant,
    val createdBy: String,
    val lastUsedAt: Instant? = null,
    val revokedAt: Instant? = null,
)

