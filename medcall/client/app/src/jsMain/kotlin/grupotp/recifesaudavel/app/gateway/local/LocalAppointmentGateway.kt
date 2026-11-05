package grupotp.recifesaudavel.app.gateway.local

import grupotp.recifesaudavel.common.scheduling.Appointment

/**
 * Copy of the signed-in person's appointments kept on the device, so they can be read offline.
 * Only one account is kept: saving for one account drops any other.
 */
interface LocalAppointmentGateway {
    suspend fun saveAppointments(accountId: Int, appointments: List<Appointment>)

    suspend fun loadAppointments(accountId: Int): List<Appointment>?
}
