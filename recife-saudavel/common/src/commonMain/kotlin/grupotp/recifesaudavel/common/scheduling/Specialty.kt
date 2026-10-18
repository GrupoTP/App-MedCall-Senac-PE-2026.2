package grupotp.recifesaudavel.common.scheduling

import kotlinx.serialization.Serializable

@Serializable
data class Specialty(
    val id: String,
    val name: String,
)
