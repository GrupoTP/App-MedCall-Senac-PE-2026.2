package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.common.clinic.ClinicAppointment

internal sealed interface Dialog {
    data class Detail(val id: Int) : Dialog
    data class Decline(val appointment: ClinicAppointment) : Dialog
    data class Reschedule(val appointment: ClinicAppointment) : Dialog
}

