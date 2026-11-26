package grupotp.medcall.common.admin

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** One line of the audit log: who did what, never the data itself. */
@Serializable
data class AuditEntry(
    val occurredAt: Instant,
    val actorName: String,
    val action: String,
    val target: String,
    val clinicId: Int? = null,
)

