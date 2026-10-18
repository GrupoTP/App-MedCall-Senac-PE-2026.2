package grupotp.recifesaudavel.common.pretriage

import kotlinx.serialization.Serializable

/**
 * The latest pre-triage that the next booking may attach, or null when there is none:
 * the patient never sent one, or the latest is already attached to an active appointment.
 */
@Serializable
data class PendingPreTriage(
    val preTriage: PreTriage? = null,
)
