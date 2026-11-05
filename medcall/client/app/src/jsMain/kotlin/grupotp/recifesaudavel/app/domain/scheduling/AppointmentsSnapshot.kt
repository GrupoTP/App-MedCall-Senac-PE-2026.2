package grupotp.recifesaudavel.app.domain.scheduling

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.common.scheduling.Appointment

/**
 * @property offline true when the list came from the device because the server was unreachable.
 * @property error why the server refused the list, if it did. The device copy is still shown.
 */
data class AppointmentsSnapshot(
    val appointments: List<Appointment>,
    val offline: Boolean,
    val error: RemoteError? = null,
)
