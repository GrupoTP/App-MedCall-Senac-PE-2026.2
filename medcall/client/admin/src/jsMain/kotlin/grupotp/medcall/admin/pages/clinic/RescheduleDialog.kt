package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.common.clinic.ProfessionalFreeDay
import grupotp.medcall.common.clinic.RescheduleRequest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import react.FC
import react.Key
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.select
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.dom.ElementId

internal val RescheduleDialog = FC<ActionDialogProps> { props ->
    val session = useSession()
    var days by useState<List<ProfessionalFreeDay>?>(null)
    var date by useState<LocalDate?>(null)
    var time by useState<LocalTime?>(null)
    var busy by useState(false)
    var error by useState<String?>(null)
    val a = props.appointment
    useEffect(a.id) {
        try {
            val free = session.api.freeDays(a.professionalId)
            days = free
            date = free.firstOrNull()?.date
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }
    modal(
        title = "Remarcar consulta",
        onClose = props.onClose,
        size = "modal-lg",
        footer = {
            button { type = Kinds.button; className = ClassName("btn btn-outline-secondary"); onClick = { props.onClose() }; +"Voltar" }
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary")
                disabled = date == null || time == null || busy
                onClick = {
                    busy = true
                    appScope.launch {
                        try {
                            session.api.reschedule(a.id, RescheduleRequest(date!!, time!!))
                            session.notify("Consulta remarcada e confirmada. O paciente vê o novo horário no app.", false)
                            props.onDone()
                        } catch (failure: ApiException) {
                            error = failure.message
                            busy = false
                        }
                    }
                }
                +"Remarcar"
            }
        },
    ) {
        p { +"${a.patientName} · marcada para ${Format.weekdayDate(a.date)} às ${Format.time(a.time)} com ${a.professionalName}." }
        errorAlert(error)
        val free = days
        if (free == null) {
            if (error == null) loading("Buscando horários livres…")
            return@modal
        }
        if (free.isEmpty()) {
            p { className = ClassName("text-secondary"); +"Este profissional não tem horários livres nos próximos 14 dias." }
            return@modal
        }
        label { className = ClassName("form-label"); htmlFor = ElementId("reschedule-day"); +"Dia" }
        select {
            id = ElementId("reschedule-day")
            className = ClassName("form-select mb-3")
            value = date?.toString() ?: ""
            onChange = { event -> date = LocalDate.parse(event.target.value); time = null }
            free.forEach { option { key = Key(it.date.toString()); value = it.date.toString(); +"${Format.weekdayDate(it.date)} (${it.times.size} livres)" } }
        }
        div { className = ClassName("form-label"); +"Horário" }
        div {
            className = ClassName("d-flex flex-wrap gap-2")
            free.firstOrNull { it.date == date }?.times.orEmpty().forEach { option ->
                button {
                    key = Key(option.toString())
                    type = Kinds.button
                    className = ClassName("btn btn-sm ${if (option == time) "btn-primary" else "btn-outline-primary"}")
                    onClick = { time = option }
                    +Format.time(option)
                }
            }
        }
    }
}

