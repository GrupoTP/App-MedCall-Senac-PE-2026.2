package grupotp.recifesaudavel.common.scheduling

import kotlinx.serialization.Serializable

/**
 * Like [specialtyId], [clinicId] points into a list sent once ([Clinic]s come from
 * `/api/v1/clinics`), so professionals of the same clinic do not repeat it.
 *
 * @property clinicId where the professional attends; its time zone rules their agenda.
 */
@Serializable
data class Professional(
    val id: Int,
    val name: String,
    val specialtyId: Int,
    val clinicId: Int,
)
