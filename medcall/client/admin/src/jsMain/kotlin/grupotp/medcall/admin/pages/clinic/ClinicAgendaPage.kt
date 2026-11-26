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
import grupotp.medcall.admin.ui.statusBadge
import grupotp.medcall.common.clinic.ClinicAppointment
import grupotp.medcall.common.clinic.ClinicCatalog
import grupotp.medcall.common.scheduling.AppointmentStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import react.ChildrenBuilder
import react.FC
import react.Key
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.useEffect
import react.useEffectOnce
import react.useState
import web.cssom.ClassName
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

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


@Suppress("unused")
private fun Session.pageLink() = Router.href(Page.ClinicAgenda)

