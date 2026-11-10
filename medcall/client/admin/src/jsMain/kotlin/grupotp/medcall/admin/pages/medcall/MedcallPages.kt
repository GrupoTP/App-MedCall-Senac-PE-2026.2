package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.admin.Page
import grupotp.medcall.admin.Router
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.api.PeriodChoice
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.pages.NewUserDialog
import grupotp.medcall.admin.pages.StaffTable
import grupotp.medcall.admin.pages.clinic.clinicDashboardBody
import grupotp.medcall.admin.pages.field
import grupotp.medcall.admin.pages.resetResult
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Brand
import grupotp.medcall.admin.ui.ChartView
import grupotp.medcall.admin.ui.Charts
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.Tone
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.idFilter
import grupotp.medcall.admin.ui.kpiBox
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.admin.ui.periodFilter
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.admin.ManagedProfessional
import grupotp.medcall.common.admin.SaveClinicRequest
import grupotp.medcall.common.admin.SaveProfessionalRequest
import grupotp.medcall.common.kpi.ClinicDashboard
import grupotp.medcall.common.kpi.ClinicRanking
import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.kpi.IntegrationMode
import grupotp.medcall.common.kpi.PlatformDashboard
import grupotp.medcall.common.kpi.Rate
import grupotp.medcall.common.scheduling.Specialty
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.common.staff.StaffUserWithPassword
import kotlinx.coroutines.launch
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.dom.html.ReactHTML.ul
import react.useEffect
import react.useEffectOnce
import react.useState
import web.cssom.ClassName
import web.dom.ElementId
import web.navigator.navigator

// ---------------------------------------------------------------------------------------------
// Visão geral (SPM-92)
// ---------------------------------------------------------------------------------------------

/** Painel MedCall: platform KPIs, only aggregates (SPM-92, SPM-96). */
val MedcallOverviewPage = FC {
    val session = useSession()
    var period by useState(PeriodChoice(30))
    var clinicFilter by useState<Int?>(null)
    var clinics by useState<List<ManagedClinic>>(emptyList())
    var data by useState<PlatformDashboard?>(null)
    var error by useState<String?>(null)
    var reload by useState(0)

    useEffectOnce { clinics = runCatching { session.api.clinics() }.getOrDefault(emptyList()) }
    useEffect(period, clinicFilter, reload) {
        try {
            data = session.api.platformDashboard(period, listOfNotNull(clinicFilter))
            error = null
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }

    div {
        className = ClassName("filter-bar row g-2 align-items-end mb-4 mx-0")
        periodFilter(period) { period = it }
        idFilter("overview-clinic", "Clínica", "Todas as clínicas", clinics.map { it.id to it.name }, clinicFilter) { clinicFilter = it }
        div {
            className = ClassName("col-12 col-lg text-lg-end small text-secondary")
            i { className = ClassName("bi bi-shield-lock me-1") }
            +"Só números agregados. Grupos com menos de ${Rate.MIN_GROUP} casos aparecem como “< ${Rate.MIN_GROUP}”."
        }
    }
    errorAlert(error) { reload++ }
    val current = data
    if (current == null) {
        if (error == null) loading("Calculando os indicadores da plataforma…")
        return@FC
    }
    div {
        className = ClassName("row g-3 mb-4")
        fun col(block: ChildrenBuilder.() -> Unit) = div { className = ClassName("col-12 col-sm-6 col-xl-4 col-xxl-2"); block() }
        col {
            kpiBox("M01", "Novos cadastros", Format.integer(current.newPatients), "vs. período anterior", "bi-person-plus", Tone.Blue,
                "Contas de pacientes criadas no período (sem as que pediram exclusão).",
                trend = Format.variation(current.newPatients, current.newPatientsPrevious))
        }
        col {
            kpiBox("M02", "Pacientes que agendaram", Format.integer(current.patientsWhoBooked), "vs. período anterior", "bi-people", Tone.Teal,
                "Pacientes distintos com pelo menos um agendamento no período. É o melhor sinal de uso disponível hoje.",
                trend = Format.variation(current.patientsWhoBooked, current.patientsWhoBookedPrevious))
        }
        col {
            kpiBox("M03", "Agendamentos", Format.integer(current.appointments), "vs. período anterior", "bi-calendar-plus", Tone.Green,
                "Agendamentos feitos no período em todas as clínicas filtradas.",
                trend = Format.variation(current.appointments, current.appointmentsPrevious))
        }
        col {
            kpiBox("M04", "Clínicas ativas", "${current.activeClinics} de ${current.totalClinics}", "com agendamento no período", "bi-hospital", Tone.Purple,
                "Clínicas com pelo menos um agendamento no período, sobre as clínicas com situação ativa.")
        }
        col {
            kpiBox("M05", "Tempo até responder", Format.hours(current.medianConfirmationHours), "mediana da plataforma", "bi-stopwatch", Tone.Amber,
                "Mediana das horas entre o agendamento e a primeira resposta da clínica.",
                alert = (current.medianConfirmationHours ?: 0.0) > 24)
        }
        col {
            kpiBox("M06", "Uso da pré-triagem", Format.percent(current.preTriageUsage), Format.ratio(current.preTriageUsage), "bi-clipboard2-pulse", Tone.Red,
                "Agendamentos com pré-triagem preenchida ÷ agendamentos. Só a contagem; o conteúdo nunca é lido.")
        }
    }
    div {
        className = ClassName("row")
        div {
            className = ClassName("col-12 col-xl-8")
            card(title = "Crescimento semanal", question = "Novos cadastros e agendamentos por semana. Semanas com * estão cortadas pelo período e têm menos dias.") {
                ChartView {
                    height = 290
                    description = "Cadastros e agendamentos por semana"
                    config = Charts.config(
                        type = "bar",
                        labels = current.weekly.map { weekLabel(it.weekStart, current.from, current.to) },
                        datasets = listOf(
                            Charts.dataset("Agendamentos", current.weekly.map { it.appointments }, Brand.BLUE),
                            Charts.dataset("Novos cadastros", current.weekly.map { it.newPatients }, Brand.GREEN, type = "line", extra = js("({ yAxisID: 'y1' })")),
                        ),
                    ).also { config ->
                        config.asDynamic().options.scales.y1 = js("({ position: 'right', beginAtZero: true, grid: { display: false } })")
                    }
                }
            }
        }
        div {
            className = ClassName("col-12 col-xl-4")
            card(title = "Integração das clínicas (M07)", question = "Clínicas ativas por forma de responder aos agendamentos.") {
                ChartView {
                    height = 220
                    description = "Clínicas por modo de integração"
                    config = Charts.doughnut(
                        current.integrationModes.map { if (it.mode == IntegrationMode.Webhook) "Webhook + API" else "Manual (portal)" },
                        current.integrationModes.map { it.clinics },
                        listOf(Brand.BLUE, Brand.GREEN),
                    )
                }
                div {
                    className = ClassName("small text-secondary mt-3")
                    +"Webhooks entregues (M08): ${Format.percent(current.webhookDeliveries)} (${Format.ratio(current.webhookDeliveries)})"
                    if (current.deadLetters > 0) span { className = ClassName("text-danger ms-1"); +"· ${current.deadLetters} na fila de mortas" }
                }
            }
        }
    }
    card(
        title = "Clínicas lado a lado",
        question = "Em que clínica agir? Clique para ver os indicadores de uma clínica.",
        bodyClass = "card-body p-0",
    ) {
        rankingTable(current.clinics)
    }
    div {
        className = ClassName("limits mb-4")
        strong { +"Limites dos dados" }
        ul {
            className = ClassName("mb-0 mt-1")
            li { +"Ambiente de demonstração: dados fictícios." }
            li { +"“Pacientes que agendaram” não conta quem só abriu o app; o número de ativos de verdade (M09) precisa de uma fonte nova." }
            li { +"Clínicas maiores tendem a ter mais agendamentos: compare também a ocupação." }
        }
    }
}

private fun ChildrenBuilder.rankingTable(rows: List<ClinicRanking>) {
    if (rows.isEmpty()) {
        emptyState("bi-hospital", "Nenhuma clínica cadastrada.")
        return
    }
    div {
        className = ClassName("table-responsive")
        table {
            className = ClassName("table table-hover align-middle mb-0")
            thead {
                tr {
                    th { +"Clínica" }
                    th { className = ClassName("text-end"); +"Agendamentos" }
                    th { className = ClassName("text-end"); +"Ocupação" }
                    th { className = ClassName("text-end"); +"Confirmação" }
                    th { className = ClassName("text-end"); +"Resposta (mediana)" }
                    th { className = ClassName("text-end"); +"Faltas" }
                    th { className = ClassName("text-end"); +"Pendentes > 24 h" }
                }
            }
            tbody {
                rows.forEach { row ->
                    tr {
                        key = Key(row.clinicId.toString())
                        td {
                            a {
                                className = ClassName("fw-semibold")
                                href = Router.href(Page.MedcallClinics, row.clinicId.toString())
                                +row.name
                            }
                            div {
                                className = ClassName("small text-secondary")
                                +row.city
                                span { className = ClassName("ms-2"); statusPill(row.status) }
                                span {
                                    className = ClassName("badge text-bg-light border ms-1")
                                    +(if (row.integrationMode == IntegrationMode.Webhook) "webhook" else "manual")
                                }
                            }
                        }
                        td { className = ClassName("text-end"); +(row.appointments?.let { Format.integer(it) } ?: "< ${Rate.MIN_GROUP}") }
                        td { className = ClassName("text-end"); +Format.percent(row.occupancy) }
                        td { className = ClassName("text-end"); +Format.percent(row.confirmation) }
                        td {
                            className = ClassName("text-end${if ((row.medianConfirmationHours ?: 0.0) > 24) " text-danger fw-semibold" else ""}")
                            +Format.hours(row.medianConfirmationHours)
                        }
                        td { className = ClassName("text-end"); +Format.percent(row.noShow) }
                        td {
                            className = ClassName("text-end${if ((row.pendingOver24h ?: 0) > 0) " text-danger fw-semibold" else ""}")
                            +(row.pendingOver24h?.toString() ?: "< ${Rate.MIN_GROUP}")
                        }
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.statusPill(status: ClinicStatus) {
    span {
        className = ClassName(
            "badge " + when (status) {
                ClinicStatus.Active -> "text-bg-success"
                ClinicStatus.Draft -> "text-bg-warning"
                ClinicStatus.Inactive -> "text-bg-secondary"
            },
        )
        +when (status) {
            ClinicStatus.Active -> "ativa"
            ClinicStatus.Draft -> "rascunho"
            ClinicStatus.Inactive -> "inativa"
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Clínicas (SPM-93, SPM-95)
// ---------------------------------------------------------------------------------------------

external interface ClinicsProps : Props {
    var clinicId: Int?
}

val MedcallClinicsPage = FC<ClinicsProps> { props ->
    val id = props.clinicId
    if (id != null) {
        ClinicDetail { clinicId = id }
    } else {
        ClinicList()
    }
}

private val ClinicList = FC {
    val session = useSession()
    var clinics by useState<List<ManagedClinic>?>(null)
    var error by useState<String?>(null)
    var reload by useState(0)
    var editing by useState<ManagedClinic?>(null)
    var creating by useState(false)

    useEffect(reload) {
        try {
            clinics = session.api.clinics()
            error = null
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }
    errorAlert(error) { reload++ }
    card(
        title = "Clínicas parceiras",
        question = "Clínica nova começa como rascunho e só aparece no app quando ativa (com profissional e administrador).",
        tools = {
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary btn-sm")
                onClick = { creating = true }
                i { className = ClassName("bi bi-plus-lg me-1") }
                +"Nova clínica"
            }
        },
        bodyClass = "card-body p-0",
    ) {
        val list = clinics
        when {
            list == null -> loading()
            list.isEmpty() -> emptyState("bi-hospital", "Nenhuma clínica ainda.")
            else -> div {
                className = ClassName("table-responsive")
                table {
                    className = ClassName("table table-hover align-middle mb-0")
                    thead {
                        tr {
                            th { +"Clínica" }
                            th { +"Situação" }
                            th { className = ClassName("d-none d-md-table-cell"); +"Fuso" }
                            th { className = ClassName("text-end"); +"Profissionais" }
                            th { className = ClassName("text-end"); +"Administradores" }
                            th { className = ClassName("d-none d-lg-table-cell"); +"Último agendamento" }
                            th { className = ClassName("text-end"); +"Ações" }
                        }
                    }
                    tbody {
                        list.forEach { clinic ->
                            tr {
                                key = Key(clinic.id.toString())
                                td {
                                    a { className = ClassName("fw-semibold"); href = Router.href(Page.MedcallClinics, clinic.id.toString()); +clinic.name }
                                    div { className = ClassName("small text-secondary"); +listOf(clinic.city, clinic.address).filter { it.isNotBlank() }.joinToString(" · ") }
                                }
                                td {
                                    statusPill(clinic.status)
                                    span { className = ClassName("badge text-bg-light border ms-1"); +(if (clinic.integrationMode == IntegrationMode.Webhook) "webhook" else "manual") }
                                }
                                td { className = ClassName("d-none d-md-table-cell small"); +clinic.timeZoneId }
                                td { className = ClassName("text-end"); +clinic.professionals.toString() }
                                td { className = ClassName("text-end${if (clinic.admins == 0) " text-danger" else ""}"); +clinic.admins.toString() }
                                td { className = ClassName("d-none d-lg-table-cell small text-secondary"); +(clinic.lastAppointmentAt?.let { Format.dateTime(it) } ?: "—") }
                                td {
                                    className = ClassName("text-end text-nowrap")
                                    button {
                                        type = Kinds.button
                                        className = ClassName("btn btn-sm btn-outline-secondary")
                                        onClick = { editing = clinic }
                                        i { className = ClassName("bi bi-pencil") }
                                        span { className = ClassName("d-none d-xl-inline ms-1"); +"Editar" }
                                    }
                                    statusButton(clinic) { status ->
                                        appScope.launch {
                                            try {
                                                session.api.setClinicStatus(clinic.id, status)
                                                session.notify("Situação de ${clinic.name} atualizada.", false)
                                            } catch (failure: ApiException) {
                                                session.notify(failure.message, true)
                                            }
                                            reload++
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (creating || editing != null) {
        ClinicForm {
            clinic = editing
            onClose = { creating = false; editing = null }
            onSaved = { creating = false; editing = null; reload++ }
        }
    }
}

private fun ChildrenBuilder.statusButton(clinic: ManagedClinic, onChange: (ClinicStatus) -> Unit) {
    val (target, text, css) = when (clinic.status) {
        ClinicStatus.Active -> Triple(ClinicStatus.Inactive, "Desativar", "btn-outline-danger")
        else -> Triple(ClinicStatus.Active, "Ativar", "btn-outline-success")
    }
    button {
        type = Kinds.button
        className = ClassName("btn btn-sm $css ms-1")
        onClick = { onChange(target) }
        i { className = ClassName("bi ${if (target == ClinicStatus.Active) "bi-toggle-on" else "bi-toggle-off"}") }
        span { className = ClassName("d-none d-xl-inline ms-1"); +text }
    }
}

external interface ClinicFormProps : Props {
    var clinic: ManagedClinic?
    var onClose: () -> Unit
    var onSaved: () -> Unit
}

private val ClinicForm = FC<ClinicFormProps> { props ->
    val session = useSession()
    val existing = props.clinic
    var name by useState(existing?.name ?: "")
    var city by useState(existing?.city ?: "")
    var zone by useState(existing?.timeZoneId ?: "America/Recife")
    var address by useState(existing?.address ?: "")
    var phone by useState(existing?.phone ?: "")
    var zones by useState(listOf("America/Recife"))
    var busy by useState(false)
    var error by useState<String?>(null)
    useEffectOnce { zones = runCatching { session.api.timeZones() }.getOrDefault(zones) }

    modal(
        if (existing == null) "Nova clínica" else "Editar ${existing.name}",
        props.onClose,
        footer = {
            button { type = Kinds.button; className = ClassName("btn btn-outline-secondary"); onClick = { props.onClose() }; +"Cancelar" }
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary")
                disabled = busy || name.isBlank() || city.isBlank()
                onClick = {
                    busy = true
                    appScope.launch {
                        try {
                            val request = SaveClinicRequest(name, city, zone, address, phone)
                            if (existing == null) session.api.createClinic(request) else session.api.updateClinic(existing.id, request)
                            session.notify(if (existing == null) "Clínica cadastrada como rascunho." else "Clínica atualizada.", false)
                            props.onSaved()
                        } catch (failure: ApiException) {
                            error = failure.message
                            busy = false
                        }
                    }
                }
                +"Salvar"
            }
        },
    ) {
        errorAlert(error)
        field("clinic-name", "Nome", Kinds.text, name) { name = it }
        div {
            className = ClassName("row")
            div { className = ClassName("col-sm-6"); field("clinic-city", "Cidade", Kinds.text, city) { city = it } }
            div {
                className = ClassName("col-sm-6 mb-3")
                label { className = ClassName("form-label"); htmlFor = ElementId("clinic-zone"); +"Fuso horário" }
                select {
                    id = ElementId("clinic-zone")
                    className = ClassName("form-select")
                    value = zone
                    onChange = { event -> zone = event.target.value }
                    (zones + zone).distinct().forEach { option { key = Key(it); value = it; +it.removePrefix("America/").replace('_', ' ') } }
                }
                div { className = ClassName("form-text"); +"Horários da agenda são sempre no relógio da clínica." }
            }
        }
        field("clinic-address", "Endereço", Kinds.text, address, required = false) { address = it }
        field("clinic-phone", "Telefone", Kinds.text, phone, required = false) { phone = it }
    }
}

external interface ClinicDetailProps : Props {
    var clinicId: Int
}

/** One clinic in the panel: its KPIs (masked) and its team, never its appointments. */
private val ClinicDetail = FC<ClinicDetailProps> { props ->
    val session = useSession()
    var clinic by useState<ManagedClinic?>(null)
    var period by useState(PeriodChoice(30))
    var dashboard by useState<ClinicDashboard?>(null)
    var team by useState<List<StaffUser>?>(null)
    var error by useState<String?>(null)
    var reloadTeam by useState(0)
    var creating by useState(false)
    var reset by useState<StaffUserWithPassword?>(null)

    useEffect(props.clinicId) {
        clinic = runCatching { session.api.clinics().firstOrNull { it.id == props.clinicId } }.getOrNull()
    }
    useEffect(props.clinicId, period) {
        try {
            dashboard = session.api.clinicDashboardForMedcall(props.clinicId, period)
            error = null
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }
    useEffect(props.clinicId, reloadTeam) {
        team = runCatching { session.api.clinicTeam(props.clinicId) }.getOrNull()
    }

    div {
        className = ClassName("d-flex flex-wrap align-items-center gap-2 mb-3")
        a { className = ClassName("btn btn-sm btn-outline-secondary"); href = Router.href(Page.MedcallClinics); i { className = ClassName("bi bi-arrow-left me-1") }; +"Clínicas" }
        val current = clinic
        if (current != null) {
            h4 { className = ClassName("mb-0 ms-2"); +current.name }
            statusPill(current.status)
            span { className = ClassName("text-secondary small"); +"${current.city} · ${current.timeZoneId}" }
        }
    }
    card(
        title = "Equipe da clínica",
        question = "A MedCall cria o primeiro administrador; depois a própria clínica cadastra a equipe no portal.",
        tools = {
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary btn-sm")
                onClick = { creating = true }
                i { className = ClassName("bi bi-person-plus me-1") }
                +"Novo acesso"
            }
        },
        bodyClass = "card-body p-0",
    ) {
        val list = team
        if (list == null) {
            loading()
        } else {
            StaffTable {
                users = list
                me = session.account
                onActive = { user, active ->
                    appScope.launch {
                        try {
                            session.api.setUserActive(user.id, active)
                            session.notify(if (active) "${user.fullName} reativado." else "${user.fullName} desativado.", false)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        }
                        reloadTeam++
                    }
                }
                onReset = { user ->
                    appScope.launch {
                        try {
                            reset = session.api.resetPasswordByMedcall(user.id)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        }
                    }
                }
            }
        }
    }
    div {
        className = ClassName("filter-bar row g-2 align-items-end mb-4 mx-0")
        periodFilter(period) { period = it }
        div {
            className = ClassName("col-12 col-lg text-lg-end small text-secondary")
            i { className = ClassName("bi bi-shield-lock me-1") }
            +"Indicadores agregados da clínica. A MedCall não vê a lista de agendamentos nem dados de pacientes."
        }
    }
    errorAlert(error)
    val current = dashboard
    if (current == null) {
        if (error == null) loading()
    } else {
        clinicDashboardBody(current, pendingHref = null)
    }
    if (creating) {
        NewUserDialog {
            title = "Novo acesso para ${clinic?.name ?: "a clínica"}"
            roles = listOf(StaffRole.ClinicAdmin, StaffRole.Receptionist)
            onCreate = { request -> session.api.createUserForClinic(props.clinicId, request).also { reloadTeam++ } }
            onClose = { creating = false }
        }
    }
    resetResult(reset, { navigator.clipboard.writeTextAsync(it); session.notify("Senha copiada.", false) }) { reset = null }
}

// ---------------------------------------------------------------------------------------------
// Profissionais (SPM-94)
// ---------------------------------------------------------------------------------------------

val MedcallProfessionalsPage = FC {
    val session = useSession()
    var clinics by useState<List<ManagedClinic>>(emptyList())
    var specialties by useState<List<Specialty>>(emptyList())
    var clinicFilter by useState<Int?>(null)
    var professionals by useState<List<ManagedProfessional>?>(null)
    var reload by useState(0)
    var editing by useState<ManagedProfessional?>(null)
    var creating by useState(false)
    var newSpecialty by useState("")

    useEffect(reload) {
        clinics = runCatching { session.api.clinics() }.getOrDefault(emptyList())
        specialties = runCatching { session.api.specialties() }.getOrDefault(emptyList())
    }
    useEffect(clinicFilter, reload) {
        professionals = runCatching { session.api.professionals(clinicFilter) }.getOrNull()
    }
    val clinicNames = clinics.associate { it.id to it.name }

    div {
        className = ClassName("row")
        div {
            className = ClassName("col-12 col-xl-8")
            card(
                title = "Profissionais",
                question = "Profissional inativo deixa de oferecer horários no app; as consultas já marcadas continuam.",
                tools = {
                    button {
                        type = Kinds.button
                        className = ClassName("btn btn-primary btn-sm")
                        disabled = clinics.isEmpty() || specialties.isEmpty()
                        onClick = { creating = true }
                        i { className = ClassName("bi bi-plus-lg me-1") }
                        +"Novo profissional"
                    }
                },
                bodyClass = "card-body p-0",
            ) {
                div {
                    className = ClassName("p-3 pb-0 row")
                    idFilter("prof-clinic", "Clínica", "Todas", clinics.map { it.id to it.name }, clinicFilter, width = "col-12 col-md-6") { clinicFilter = it }
                }
                val list = professionals
                when {
                    list == null -> loading()
                    list.isEmpty() -> emptyState("bi-person-badge", "Nenhum profissional.")
                    else -> div {
                        className = ClassName("table-responsive mt-2")
                        table {
                            className = ClassName("table table-hover align-middle mb-0")
                            thead { tr { th { +"Profissional" }; th { +"Especialidade" }; th { +"Clínica" }; th { +"Situação" }; th { className = ClassName("text-end"); +"" } } }
                            tbody {
                                list.forEach { professional ->
                                    tr {
                                        key = Key(professional.id.toString())
                                        td {
                                            strong { +professional.name }
                                            if (professional.registration.isNotBlank()) div { className = ClassName("small text-secondary"); +professional.registration }
                                        }
                                        td { +professional.specialtyName }
                                        td { +(clinicNames[professional.clinicId] ?: "—") }
                                        td {
                                            span {
                                                className = ClassName("badge ${if (professional.active) "text-bg-success" else "text-bg-secondary"}")
                                                +(if (professional.active) "Ativo" else "Inativo")
                                            }
                                        }
                                        td {
                                            className = ClassName("text-end")
                                            button {
                                                type = Kinds.button
                                                className = ClassName("btn btn-sm btn-outline-secondary")
                                                onClick = { editing = professional }
                                                i { className = ClassName("bi bi-pencil") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        div {
            className = ClassName("col-12 col-xl-4")
            card(title = "Especialidades", question = "Lista única para todas as clínicas.") {
                div {
                    className = ClassName("d-flex flex-wrap gap-1 mb-3")
                    specialties.forEach { span { key = Key(it.id.toString()); className = ClassName("badge text-bg-light border fw-normal"); +it.name } }
                }
                div {
                    className = ClassName("input-group")
                    input {
                        className = ClassName("form-control")
                        placeholder = "Nova especialidade"
                        asDynamic()["aria-label"] = "Nova especialidade"
                        value = newSpecialty
                        onChange = { event -> newSpecialty = event.target.value }
                    }
                    button {
                        type = Kinds.button
                        className = ClassName("btn btn-outline-primary")
                        disabled = newSpecialty.isBlank()
                        onClick = {
                            appScope.launch {
                                try {
                                    session.api.createSpecialty(newSpecialty.trim())
                                    session.notify("Especialidade criada.", false)
                                    newSpecialty = ""
                                    reload++
                                } catch (failure: ApiException) {
                                    session.notify(failure.message, true)
                                }
                            }
                        }
                        +"Adicionar"
                    }
                }
            }
        }
    }
    if (creating || editing != null) {
        ProfessionalForm {
            professional = editing
            this.clinics = clinics
            this.specialties = specialties
            defaultClinic = clinicFilter
            onClose = { creating = false; editing = null }
            onSaved = { creating = false; editing = null; reload++ }
        }
    }
}

external interface ProfessionalFormProps : Props {
    var professional: ManagedProfessional?
    var clinics: List<ManagedClinic>
    var specialties: List<Specialty>
    var defaultClinic: Int?
    var onClose: () -> Unit
    var onSaved: () -> Unit
}

private val ProfessionalForm = FC<ProfessionalFormProps> { props ->
    val session = useSession()
    val existing = props.professional
    var name by useState(existing?.name ?: "")
    var registration by useState(existing?.registration ?: "")
    var specialtyId by useState(existing?.specialtyId ?: props.specialties.first().id)
    var clinicId by useState(existing?.clinicId ?: props.defaultClinic ?: props.clinics.first().id)
    var active by useState(existing?.active ?: true)
    var busy by useState(false)
    var error by useState<String?>(null)

    modal(
        if (existing == null) "Novo profissional" else "Editar ${existing.name}",
        props.onClose,
        footer = {
            button { type = Kinds.button; className = ClassName("btn btn-outline-secondary"); onClick = { props.onClose() }; +"Cancelar" }
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary")
                disabled = busy || name.isBlank()
                onClick = {
                    busy = true
                    appScope.launch {
                        try {
                            val request = SaveProfessionalRequest(name, registration, specialtyId, clinicId, active)
                            if (existing == null) session.api.createProfessional(request) else session.api.updateProfessional(existing.id, request)
                            session.notify("Profissional salvo.", false)
                            props.onSaved()
                        } catch (failure: ApiException) {
                            error = failure.message
                            busy = false
                        }
                    }
                }
                +"Salvar"
            }
        },
    ) {
        errorAlert(error)
        field("prof-name", "Nome", Kinds.text, name) { name = it }
        field("prof-registration", "Registro no conselho (CRM, CRP...)", Kinds.text, registration, required = false) { registration = it }
        div {
            className = ClassName("row")
            selectField("prof-specialty", "Especialidade", props.specialties.map { it.id to it.name }, specialtyId) { specialtyId = it }
            selectField("prof-clinic", "Clínica", props.clinics.map { it.id to it.name }, clinicId) { clinicId = it }
        }
        div {
            className = ClassName("form-check form-switch")
            input {
                id = ElementId("prof-active")
                type = Kinds.checkbox
                className = ClassName("form-check-input")
                checked = active
                onChange = { event -> active = event.target.checked }
            }
            label { className = ClassName("form-check-label"); htmlFor = ElementId("prof-active"); +"Ativo (oferece horários no app)" }
        }
    }
}

private fun ChildrenBuilder.selectField(id: String, text: String, options: List<Pair<Int, String>>, selected: Int, onPick: (Int) -> Unit) {
    div {
        className = ClassName("col-sm-6 mb-3")
        label { className = ClassName("form-label"); htmlFor = ElementId(id); +text }
        select {
            this.id = ElementId(id)
            className = ClassName("form-select")
            value = selected.toString()
            onChange = { event -> event.target.value.toIntOrNull()?.let(onPick) }
            options.forEach { (value, label) -> option { key = Key(value.toString()); this.value = value.toString(); +label } }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Usuários das clínicas (SPM-95)
// ---------------------------------------------------------------------------------------------

val MedcallUsersPage = FC {
    val session = useSession()
    var users by useState<List<StaffUser>?>(null)
    var clinics by useState<List<ManagedClinic>>(emptyList())
    var reload by useState(0)
    var reset by useState<StaffUserWithPassword?>(null)
    useEffect(reload) {
        users = runCatching { session.api.allClinicStaff() }.getOrNull()
        clinics = runCatching { session.api.clinics() }.getOrDefault(emptyList())
    }
    card(
        title = "Todas as contas do Portal da Clínica",
        question = "Para incidentes: desative uma conta ou gere uma senha nova. A gestão do dia a dia é da própria clínica.",
        bodyClass = "card-body p-0",
    ) {
        val list = users
        if (list == null) {
            loading()
        } else {
            StaffTable {
                this.users = list
                me = session.account
                clinicNames = clinics.associate { it.id to it.name }
                onActive = { user, active ->
                    appScope.launch {
                        try {
                            session.api.setUserActive(user.id, active)
                            session.notify(if (active) "${user.fullName} reativado." else "${user.fullName} desativado.", false)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        }
                        reload++
                    }
                }
                onReset = { user ->
                    appScope.launch {
                        try {
                            reset = session.api.resetPasswordByMedcall(user.id)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        }
                    }
                }
            }
        }
    }
    resetResult(reset, { navigator.clipboard.writeTextAsync(it); session.notify("Senha copiada.", false) }) { reset = null }
    p { className = ClassName("small text-secondary"); +"O cadastro de um novo acesso fica na página da clínica (Clínicas › nome da clínica)." }
}

/** "sem. 12/out", with a star when the period cuts the week. */
internal fun weekLabel(weekStart: kotlinx.datetime.LocalDate, from: kotlinx.datetime.LocalDate, to: kotlinx.datetime.LocalDate): String {
    val end = kotlinx.datetime.LocalDate.fromEpochDays(weekStart.toEpochDays() + 6)
    return "sem. " + Format.shortDate(weekStart) + if (weekStart < from || end > to) "*" else ""
}
