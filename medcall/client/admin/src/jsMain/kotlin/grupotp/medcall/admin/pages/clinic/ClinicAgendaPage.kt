package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.Page
import grupotp.medcall.admin.Router
import grupotp.medcall.admin.Session
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.idFilter
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.localToday
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.admin.ui.statusBadge
import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.clinic.ClinicAppointment
import grupotp.medcall.common.clinic.ClinicAppointmentDetail
import grupotp.medcall.common.clinic.ClinicCatalog
import grupotp.medcall.common.clinic.DeclineReason
import grupotp.medcall.common.clinic.DeclineRequest
import grupotp.medcall.common.clinic.ProfessionalFreeDay
import grupotp.medcall.common.clinic.RescheduleRequest
import grupotp.medcall.common.scheduling.AppointmentStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.dd
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.dl
import react.dom.html.ReactHTML.dt
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.textarea
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.dom.html.ReactHTML.ul
import react.useEffect
import react.useEffectOnce
import react.useState
import web.cssom.ClassName
import web.dom.ElementId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/** Quick views of the agenda. */
private enum class AgendaView(val label: String, val icon: String) {
    Today("Hoje", "bi-calendar-day"),
    Week("Próximos 7 dias", "bi-calendar-week"),
    Pending("Aguardando resposta", "bi-hourglass-split"),
    Attendance("Sem registro de comparecimento", "bi-clipboard-check"),
}

private sealed interface Dialog {
    data class Detail(val id: Int) : Dialog
    data class Decline(val appointment: ClinicAppointment) : Dialog
    data class Reschedule(val appointment: ClinicAppointment) : Dialog
}

/** Portal da Clínica: the reception's agenda (SPM-88, SPM-89, SPM-90). */
val ClinicAgendaPage = FC {
    val session = useSession()
    val initialView = if (Router.current(session.account.role).argument == "pendentes") AgendaView.Pending else AgendaView.Today
    var view by useState(initialView)
    var catalog by useState<ClinicCatalog?>(null)
    var professionalId by useState<Int?>(null)
    var list by useState<List<ClinicAppointment>?>(null)
    var error by useState<String?>(null)
    var reload by useState(0)
    var busyId by useState<Int?>(null)
    var dialog by useState<Dialog?>(null)

    useEffectOnce { catalog = runCatching { session.api.clinicCatalog() }.getOrNull() }
    useEffect(view, professionalId, reload) {
        val today = localToday()
        val day = 1L
        fun plus(days: Long) = LocalDate.fromEpochDays(today.toEpochDays() + days * day)
        while (true) {
            try {
                list = when (view) {
                    AgendaView.Today -> session.api.agenda(today, today, emptyList(), professionalId, null, false)
                    AgendaView.Week -> session.api.agenda(today, plus(6), emptyList(), professionalId, null, false)
                    AgendaView.Pending -> session.api.agenda(today, plus(30), listOf("pending"), professionalId, null, false)
                    AgendaView.Attendance -> session.api.agenda(plus(-14), today, emptyList(), professionalId, null, true)
                }
                error = null
            } catch (failure: ApiException) {
                error = failure.message
            }
            // The agenda refreshes itself every minute.
            delay(60_000)
        }
        @Suppress("UNREACHABLE_CODE")
        Unit
    }

    fun act(appointment: ClinicAppointment, success: String, call: suspend () -> ClinicAppointment) {
        busyId = appointment.id
        appScope.launch {
            try {
                call()
                session.notify(success, false)
                session.refreshPending()
            } catch (failure: ApiException) {
                session.notify(failure.message, true)
            } finally {
                busyId = null
                reload++
            }
        }
    }

    div {
        className = ClassName("filter-bar d-flex flex-wrap gap-2 align-items-end mb-4")
        div {
            className = ClassName("btn-group flex-wrap")
            asDynamic().role = "group"
            AgendaView.entries.forEach { option ->
                button {
                    key = Key(option.name)
                    type = Kinds.button
                    className = ClassName("btn ${if (option == view) "btn-primary" else "btn-outline-primary"}")
                    onClick = { view = option; list = null }
                    i { className = ClassName("bi ${option.icon} me-1") }
                    +option.label
                }
            }
        }
        div {
            className = ClassName("ms-auto")
            style = js("({ minWidth: '240px' })").unsafeCast<react.CSSProperties>()
            idFilter(
                "agenda-professional", "Profissional", "Todos os profissionais",
                catalog?.professionals.orEmpty().map { it.id to it.name }, professionalId, width = "",
            ) { professionalId = it; list = null }
        }
    }

    errorAlert(error) { reload++ }
    val current = list
    card(
        title = view.label,
        question = when (view) {
            AgendaView.Today -> "Consultas de hoje, no horário da clínica."
            AgendaView.Week -> "Consultas de hoje até daqui a 6 dias."
            AgendaView.Pending -> "Pacientes esperando a clínica confirmar. Em destaque: há mais de 24 horas."
            AgendaView.Attendance -> "Consultas confirmadas dos últimos 14 dias que já passaram e ainda não têm comparecimento registrado."
        },
        tools = {
            if (current != null) span { className = ClassName("badge text-bg-light border"); +"${current.size} agendamentos" }
        },
        bodyClass = "card-body p-0",
    ) {
        when {
            current == null -> loading()
            current.isEmpty() -> emptyState("bi-calendar-check", "Nada por aqui. Boa notícia!")
            else -> agendaTable(current, view == AgendaView.Today, busyId, { dialog = Dialog.Detail(it.id) }) { appointment, action ->
                when (action) {
                    Action.Confirm -> act(appointment, "Consulta confirmada. O paciente já vê no app.") { session.api.confirm(appointment.id) }
                    Action.Decline -> dialog = Dialog.Decline(appointment)
                    Action.Reschedule -> dialog = Dialog.Reschedule(appointment)
                    Action.Attended -> act(appointment, "Comparecimento registrado.") { session.api.attendance(appointment.id, true) }
                    Action.NoShow -> act(appointment, "Falta registrada.") { session.api.attendance(appointment.id, false) }
                    Action.Undo -> act(appointment, "Registro desfeito.") { session.api.undoAttendance(appointment.id) }
                }
            }
        }
    }

    when (val open = dialog) {
        is Dialog.Detail -> DetailDialog { id = open.id; onClose = { dialog = null } }
        is Dialog.Decline -> DeclineDialog {
            appointment = open.appointment
            onClose = { dialog = null }
            onDone = { dialog = null; reload++; session.refreshPending() }
        }
        is Dialog.Reschedule -> RescheduleDialog {
            appointment = open.appointment
            onClose = { dialog = null }
            onDone = { dialog = null; reload++; session.refreshPending() }
        }
        null -> Unit
    }
}

private enum class Action { Confirm, Decline, Reschedule, Attended, NoShow, Undo }

private fun ChildrenBuilder.agendaTable(
    list: List<ClinicAppointment>,
    timeOnly: Boolean,
    busyId: Int?,
    onOpen: (ClinicAppointment) -> Unit,
    onAction: (ClinicAppointment, Action) -> Unit,
) {
    val now = Clock.System.now()
    div {
        className = ClassName("table-responsive")
        table {
            className = ClassName("table table-hover align-middle mb-0")
            thead {
                tr {
                    th { +"Quando" }
                    th { +"Paciente" }
                    th { className = ClassName("d-none d-md-table-cell"); +"Profissional" }
                    th { +"Situação" }
                    th { className = ClassName("text-end d-none d-md-table-cell"); +"Ações" }
                }
            }
            tbody {
                list.forEach { appointment ->
                    val overdue = appointment.status == AppointmentStatus.Pending && now - appointment.createdAt > 24.hours
                    tr {
                        key = Key(appointment.id.toString())
                        className = ClassName(if (overdue) "row-overdue" else "")
                        td {
                            strong { +Format.time(appointment.time) }
                            if (!timeOnly) div { className = ClassName("small text-secondary"); +Format.weekdayDate(appointment.date) }
                        }
                        td {
                            button {
                                type = Kinds.button
                                className = ClassName("btn btn-link p-0 text-start fw-semibold")
                                onClick = { onOpen(appointment) }
                                +appointment.patientName
                            }
                            div {
                                className = ClassName("small text-secondary")
                                +appointment.patientPhone.formatted()
                                if (appointment.hasPreTriage) {
                                    span { className = ClassName("badge text-bg-info ms-2"); title = "O paciente preencheu a pré-triagem"; +"pré-triagem" }
                                }
                            }
                            // On phones the professional column is hidden: show it here.
                            div { className = ClassName("small text-secondary d-md-none"); +"${appointment.professionalName} · ${appointment.specialtyName}" }
                        }
                        td {
                            className = ClassName("d-none d-md-table-cell")
                            +appointment.professionalName
                            div { className = ClassName("small text-secondary"); +appointment.specialtyName }
                        }
                        td {
                            statusBadge(appointment.status, appointment.rescheduled)
                            if (overdue) {
                                div { className = ClassName("small text-danger mt-1 text-nowrap"); +"há ${Format.relative(appointment.createdAt, now).removePrefix("há ")} sem resposta" }
                            }
                            // On phones the actions column is hidden: the buttons go under the status.
                            div {
                                className = ClassName("d-md-none mt-2 d-flex flex-wrap gap-1")
                                actionButtons(appointment, now, busyId == appointment.id) { onAction(appointment, it) }
                            }
                        }
                        td {
                            className = ClassName("text-end text-nowrap d-none d-md-table-cell")
                            actionButtons(appointment, now, busyId == appointment.id) { onAction(appointment, it) }
                        }
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.actionButtons(appointment: ClinicAppointment, now: kotlin.time.Instant, busy: Boolean, onAction: (Action) -> Unit) {
    fun small(action: Action, text: String, css: String, icon: String) {
        button {
            type = Kinds.button
            className = ClassName("btn btn-sm $css ms-1")
            disabled = busy
            title = text
            onClick = { onAction(action) }
            i { className = ClassName("bi $icon") }
            span { className = ClassName("d-none d-xl-inline ms-1"); +text }
        }
    }
    val started = appointment.date < localToday() ||
        (appointment.date == localToday() && appointment.time <= nowTime())
    when (appointment.status) {
        AppointmentStatus.Pending -> {
            small(Action.Confirm, "Confirmar", "btn-success", "bi-check-lg")
            small(Action.Reschedule, "Remarcar", "btn-outline-secondary", "bi-arrow-left-right")
            small(Action.Decline, "Recusar", "btn-outline-danger", "bi-x-lg")
        }
        AppointmentStatus.Confirmed -> if (started) {
            small(Action.Attended, "Compareceu", "btn-outline-success", "bi-person-check")
            small(Action.NoShow, "Faltou", "btn-outline-danger", "bi-person-x")
        } else {
            small(Action.Reschedule, "Remarcar", "btn-outline-secondary", "bi-arrow-left-right")
            small(Action.Decline, "Cancelar", "btn-outline-danger", "bi-x-lg")
        }
        AppointmentStatus.Attended, AppointmentStatus.NoShow -> {
            val until = appointment.attendanceUndoUntil
            if (until != null && until > now) small(Action.Undo, "Desfazer", "btn-outline-secondary", "bi-arrow-counterclockwise")
        }
        else -> Unit
    }
}

private fun nowTime(): LocalTime {
    val now: dynamic = js("new Date()")
    return LocalTime(now.getHours() as Int, now.getMinutes() as Int)
}

external interface DetailProps : Props {
    var id: Int
    var onClose: () -> Unit
}

/** Appointment detail: reason, history of status changes and the pre-triage, if any. */
private val DetailDialog = FC<DetailProps> { props ->
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

external interface ActionDialogProps : Props {
    var appointment: ClinicAppointment
    var onClose: () -> Unit
    var onDone: () -> Unit
}

private val DeclineDialog = FC<ActionDialogProps> { props ->
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

private val RescheduleDialog = FC<ActionDialogProps> { props ->
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

@Suppress("unused")
private fun Session.pageLink() = Router.href(Page.ClinicAgenda)
