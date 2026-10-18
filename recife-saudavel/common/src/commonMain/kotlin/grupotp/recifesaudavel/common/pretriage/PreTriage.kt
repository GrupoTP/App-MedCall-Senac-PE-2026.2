package grupotp.recifesaudavel.common.pretriage

import kotlinx.serialization.Serializable

/**
 * A pre-triage the server stored.
 *
 * @property createdAt epoch milliseconds.
 * @property appointmentId appointment it was attached to when booking, if any.
 */
@Serializable
data class PreTriage(
    val id: String,
    val createdAt: Long,
    val form: PreTriageForm,
    val appointmentId: String? = null,
)
