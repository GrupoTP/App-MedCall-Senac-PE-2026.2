package grupotp.recifesaudavel.common.pretriage

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A pre-triage the server stored.
 *
 * @property appointmentId appointment it was attached to when booking, if any.
 */
@Serializable
data class PreTriage(
    val id: Int,
    val createdAt: Instant,
    val form: PreTriageForm,
    val appointmentId: Int? = null,
)
