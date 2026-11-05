package grupotp.recifesaudavel.app.fake

import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import grupotp.recifesaudavel.common.scheduling.Clinic
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Clinics and appointments shared by the scheduling tests.
 */
internal object SampleAppointments {
    val recife = Clinic(id = 1, name = "Clínica Boa Vista", city = "Recife", timeZoneId = "America/Recife")
    val manaus = Clinic(id = 2, name = "Teleconsulta Manaus", city = "Manaus", timeZoneId = "America/Manaus")
    val sooner = appointment(id = 1, date = "2026-10-14", time = "09:00")
    val later = appointment(id = 2, date = "2026-10-20", time = "14:30")

    fun appointment(id: Int, date: String, time: String, clinic: Clinic = recife) = Appointment(
        id = id,
        specialtyName = "Cardiologia",
        professionalName = "Dra. Lúcia Oliveira",
        clinic = clinic,
        date = LocalDate.parse(date),
        time = LocalTime.parse(time),
        status = AppointmentStatus.Pending,
        patientName = "Graça",
        contactPhone = Phone.parse("(81) 99999-9999"),
    )
}
