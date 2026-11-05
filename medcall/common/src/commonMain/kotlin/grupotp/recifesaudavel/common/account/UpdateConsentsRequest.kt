package grupotp.recifesaudavel.common.account

import kotlinx.serialization.Serializable

/** Only the optional consent can be changed after registration. */
@Serializable
data class UpdateConsentsRequest(
    val wantsNotifications: Boolean,
)
