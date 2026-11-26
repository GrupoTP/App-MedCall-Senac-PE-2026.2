package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.common.clinic.DeclineReason
import grupotp.medcall.common.clinic.DeclineRequest
import grupotp.medcall.common.scheduling.AppointmentStatus
import kotlinx.coroutines.launch
import react.FC
import react.Key
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.textarea
import react.useState
import web.cssom.ClassName
import web.dom.ElementId

internal val DeclineDialog = FC<ActionDialogProps> { props ->
    val session = useSession()
    var reason by useState<DeclineReason?>(null)
    var note by useState("")
    var busy by useState(false)
    var error by useState<String?>(null)
    val a = props.appointment
    modal(
        title = if (a.status == AppointmentStatus.Pending) "Recusar agendamento" else "Cancelar consulta confirmada",
        onClose = props.onClose,
        footer = {
            button { type = Kinds.button; className = ClassName("btn btn-outline-secondary"); onClick = { props.onClose() }; +"Voltar" }
            button {
                type = Kinds.button
                className = ClassName("btn btn-danger")
                disabled = reason == null || busy
                onClick = {
                    busy = true
                    appScope.launch {
                        try {
                            session.api.decline(a.id, DeclineRequest(reason!!, note.trim()))
                            session.notify("Agendamento recusado. O horário voltou a ficar livre no app.", false)
                            props.onDone()
                        } catch (failure: ApiException) {
                            error = failure.message
                            busy = false
                        }
                    }
                }
                +"Recusar"
            }
        },
    ) {
        p { +"${a.patientName} · ${Format.weekdayDate(a.date)} às ${Format.time(a.time)} com ${a.professionalName}." }
        errorAlert(error)
        label { className = ClassName("form-label"); htmlFor = ElementId("decline-reason"); +"Motivo (o paciente vê)" }
        select {
            id = ElementId("decline-reason")
            className = ClassName("form-select mb-3")
            value = reason?.name ?: ""
            onChange = { event -> reason = DeclineReason.entries.firstOrNull { it.name == event.target.value } }
            option { value = ""; +"Escolha…" }
            DeclineReason.entries.forEach { option { key = Key(it.name); value = it.name; +it.label } }
        }
        label { className = ClassName("form-label"); htmlFor = ElementId("decline-note"); +"Mensagem ao paciente (opcional)" }
        textarea {
            id = ElementId("decline-note")
            className = ClassName("form-control")
            rows = 3
            maxLength = 280
            value = note
            onChange = { event -> note = event.target.value }
        }
        small { className = ClassName("text-secondary"); +"${note.length}/280" }
    }
}

