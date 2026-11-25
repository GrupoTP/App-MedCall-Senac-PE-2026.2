package grupotp.medcall.common.auth

import kotlinx.serialization.Serializable

/** A request was accepted and no account data is returned. */
@Serializable
data class Acknowledged(
    val accepted: Boolean = true,
)
