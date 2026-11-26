package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.admin.ui.statusBadge
import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.clinic.ClinicAppointmentDetail
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.dd
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.dl
import react.dom.html.ReactHTML.dt
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.ul
import react.useEffect
import react.useState
import web.cssom.ClassName

external interface DetailProps : Props {
    var id: Int
    var onClose: () -> Unit
}


/** Appointment detail: reason, history of status changes and the pre-triage, if any. */
internal val DetailDialog = FC<DetailProps> { props ->
    val session = useSession()
    var detail by useState<ClinicAppointmentDetail?>(null)
    var error by useState<String?>(null)
    useEffect(props.id) {
        try {
            detail = session.api.appointment(props.id)
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }
    modal("Agendamento", props.onClose, size = "modal-lg") {
        errorAlert(error)
        val current = detail
        if (current == null) {
            if (error == null) loading()
            return@modal
        }
        val a = current.appointment
        dl {
            className = ClassName("row mb-3")
            fun item(label: String, value: ChildrenBuilder.() -> Unit) {
                dt { className = ClassName("col-sm-4 text-secondary fw-semibold"); +label }
                dd { className = ClassName("col-sm-8"); value() }
            }
            item("Paciente") { +a.patientName; span { className = ClassName("text-secondary ms-2"); +a.patientPhone.formatted() } }
            item("Consulta") { +"${Format.weekdayDate(a.date)} às ${Format.time(a.time)}" }
            item("Profissional") { +"${a.professionalName} · ${a.specialtyName}" }
            item("Situação") { statusBadge(a.status, a.rescheduled) }
            item("Motivo") { +a.reason.ifBlank { "Não informado" } }
            if (a.notes.isNotBlank()) item("Observações") { +a.notes }
            if (a.declineReason.isNotBlank()) item("Motivo da recusa") { +a.declineReason }
        }
        strong { +"Histórico" }
        ul {
            className = ClassName("list-unstyled small mt-2 mb-3")
            current.history.forEach { entry ->
                li {
                    className = ClassName("mb-1")
                    i { className = ClassName("bi ${historyIcon(entry.type)} me-2 text-primary") }
                    span { className = ClassName("text-secondary me-2"); +Format.dateTime(entry.occurredAt) }
                    +historyText(entry.type)
                    if (entry.actorName.isNotBlank()) span { className = ClassName("text-secondary"); +" · ${entry.actorName}" }
                    if (entry.detail.isNotBlank()) div { className = ClassName("ms-4 text-secondary"); +entry.detail }
                }
            }
        }
        val form = current.preTriage
        if (form != null) {
            strong { +"Pré-triagem do paciente" }
            p { className = ClassName("small text-secondary mb-2"); +"Respostas do paciente, sem interpretação. Dado de saúde: use só para o atendimento." }
            dl {
                className = ClassName("row small mb-0")
                fun answer(label: String, value: String?) {
                    if (value.isNullOrBlank()) return
                    dt { className = ClassName("col-sm-4 text-secondary"); +label }
                    dd { className = ClassName("col-sm-8"); +value }
                }
                answer("Motivo", form.reason)
                answer("Sintomas", form.currentSymptoms)
                answer("Intensidade", form.intensity?.let { "$it de 10" })
                answer("Doenças prévias", form.previousConditions)
                answer("Alergias", form.allergies)
                answer("Medicamentos", form.continuousMedications.ifBlank { form.regularMedications })
            }
        }
    }
}


private fun historyIcon(type: AppointmentEventType): String = when (type) {
    AppointmentEventType.Created -> "bi-plus-circle"
    AppointmentEventType.Confirmed -> "bi-check-circle"
    AppointmentEventType.Declined -> "bi-x-circle"
    AppointmentEventType.Rescheduled -> "bi-arrow-left-right"
    AppointmentEventType.Cancelled -> "bi-slash-circle"
    AppointmentEventType.Attended -> "bi-person-check"
    AppointmentEventType.NoShow -> "bi-person-x"
    AppointmentEventType.AttendanceCleared -> "bi-arrow-counterclockwise"
}


private fun historyText(type: AppointmentEventType): String = when (type) {
    AppointmentEventType.Created -> "Agendado pelo paciente no app"
    AppointmentEventType.Confirmed -> "Confirmado pela clínica"
    AppointmentEventType.Declined -> "Recusado pela clínica"
    AppointmentEventType.Rescheduled -> "Remarcado pela clínica"
    AppointmentEventType.Cancelled -> "Cancelado pelo paciente"
    AppointmentEventType.Attended -> "Paciente compareceu"
    AppointmentEventType.NoShow -> "Paciente faltou"
    AppointmentEventType.AttendanceCleared -> "Registro de comparecimento desfeito"
}

