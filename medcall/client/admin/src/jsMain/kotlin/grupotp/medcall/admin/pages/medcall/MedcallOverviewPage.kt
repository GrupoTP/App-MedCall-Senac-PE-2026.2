package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.admin.Page
import grupotp.medcall.admin.Router
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.api.PeriodChoice
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Brand
import grupotp.medcall.admin.ui.ChartView
import grupotp.medcall.admin.ui.Charts
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Tone
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.idFilter
import grupotp.medcall.admin.ui.kpiBox
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.periodFilter
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.kpi.ClinicRanking
import grupotp.medcall.common.kpi.IntegrationMode
import grupotp.medcall.common.kpi.PlatformDashboard
import grupotp.medcall.common.kpi.Rate
import react.ChildrenBuilder
import react.FC
import react.Key
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.p
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


/** "sem. 12/out", with a star when the period cuts the week. */
internal fun weekLabel(weekStart: kotlinx.datetime.LocalDate, from: kotlinx.datetime.LocalDate, to: kotlinx.datetime.LocalDate): String {
    val end = kotlinx.datetime.LocalDate.fromEpochDays(weekStart.toEpochDays() + 6)
    return "sem. " + Format.shortDate(weekStart) + if (weekStart < from || end > to) "*" else ""
}

