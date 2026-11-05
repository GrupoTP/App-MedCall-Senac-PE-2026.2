package grupotp.recifesaudavel.common.scheduling

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AppointmentStatus {
    @SerialName("pending")
    Pending,

    @SerialName("confirmed")
    Confirmed,

    @SerialName("cancelled")
    Cancelled,
}
