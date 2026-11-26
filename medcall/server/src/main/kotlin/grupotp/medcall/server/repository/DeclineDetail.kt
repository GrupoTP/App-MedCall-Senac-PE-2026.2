package grupotp.medcall.server.repository

import grupotp.medcall.common.clinic.DeclineReason
import grupotp.medcall.server.database.wireName
import grupotp.medcall.server.database.wireValue
import org.jetbrains.exposed.v1.core.and

/**
 * A decline is stored in the event's detail as `<reason>` or `<reason>|<note>`, so the dashboard
 * can count reasons and the patient still reads the clinic's note.
 */
internal object DeclineDetail {
    fun encode(reason: DeclineReason, note: String): String =
        if (note.isBlank()) reason.wireName() else "${reason.wireName()}|${note.trim()}"

    fun reasonOf(detail: String): DeclineReason? =
        runCatching { wireValue<DeclineReason>(detail.substringBefore('|')) }.getOrNull()

    /** Text for the patient: the reason's label and the note, if any. */
    fun text(detail: String): String {
        val label = reasonOf(detail)?.label ?: return ""
        val note = detail.substringAfter('|', "").trim()
        return if (note.isEmpty()) label else "$label. $note"
    }
}

