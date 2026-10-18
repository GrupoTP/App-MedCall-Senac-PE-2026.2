package grupotp.recifesaudavel.common.scheduling

import kotlinx.serialization.Serializable

/** @property clinic where the professional attends; its time zone rules their agenda. */
@Serializable
data class Professional(
    val id: String,
    val name: String,
    val specialtyId: String,
    val clinic: Clinic,
)
