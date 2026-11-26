package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.common.clinic.ClinicAppointment
import react.Props

external interface ActionDialogProps : Props {
    var appointment: ClinicAppointment
    var onClose: () -> Unit
    var onDone: () -> Unit
}

