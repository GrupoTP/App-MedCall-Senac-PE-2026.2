package grupotp.recifesaudavel.common.scheduling

import kotlinx.serialization.Serializable

@Serializable
data class Professional(
    val id: String,
    val name: String,
    val specialtyId: String,
)
