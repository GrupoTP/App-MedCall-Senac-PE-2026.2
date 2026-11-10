package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.ui.Brand
import grupotp.medcall.admin.ui.ChartView
import grupotp.medcall.admin.ui.Charts
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Tone
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.kpiBox
import grupotp.medcall.common.kpi.ClinicDashboard
import grupotp.medcall.common.kpi.Rate
import grupotp.medcall.common.kpi.Shift
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import react.ChildrenBuilder
import react.Key
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName

/**
 * Body of the clinic dashboard, shared by the clinic portal and the MedCall panel's clinic page.
 * Laid out by the three questions of the Data Science presentation (docs/kpis.md).
 *
 * @param pendingHref where the "Pendentes" box leads; null in the MedCall panel, which has no agenda.
 */
fun ChildrenBuilder.clinicDashboardBody(data: ClinicDashboard, pendingHref: String?, demoNote: Boolean = true) {
    kpiRow(data, pendingHref)

    // Question 1
    card(
        title = "1. Quais especialidades lotam a agenda primeiro, e em que dias?",
        question = "Ocupação (C02) = horários ocupados ÷ horários oferecidos, por especialidade e dia de consulta. Quanto mais escuro, mais cheio.",
    ) {
        if (data.occupancyBySpecialtyDay.isEmpty()) {
            emptyState("bi-calendar-x", "Sem horários oferecidos no período.")
        } else {
            occupancyHeatmap(data)
            div {
                className = ClassName("mt-4")
                ChartView {
                    height = 60 + 28 * data.specialties.size
                    description = "Ocupação por especialidade no período"
                    config = Charts.config(
                        type = "bar",
                        labels = data.specialties.sortedByDescending { it.occupancy.value ?: 0.0 }.map { it.name },
                        datasets = listOf(
                            Charts.dataset(
                                "Ocupação (%)",
                                data.specialties.sortedByDescending { it.occupancy.value ?: 0.0 }.map { (it.occupancy.value ?: 0.0) * 100 },
                                Brand.BLUE,
                            ),
                        ),
                        horizontal = true,
                        percentAxis = true,
                        legend = false,
                    )
                }
            }
        }
    }

    div {
        className = ClassName("row")
        // Question 2
        div {
            className = ClassName("col-12 col-xl-6")
            card(
                title = "2. A recepção está respondendo os agendamentos a tempo?",
                question = "Mediana de horas entre o agendamento e a primeira resposta da clínica (C04), por semana em que o paciente agendou. Barras: ainda sem resposta. * semana cortada pelo período.",
            ) {
                val weeks = data.confirmationByWeek
                ChartView {
                    height = 300
                    description = "Tempo mediano até a resposta, por semana"
                    config = Charts.config(
                        type = "bar",
                        labels = weeks.map { grupotp.medcall.admin.pages.medcall.weekLabel(it.weekStart, data.from, data.to) },
                        datasets = listOf(
                            Charts.dataset("Mediana até a resposta (h)", weeks.map { it.medianHours }, Brand.BLUE, type = "line"),
                            Charts.dataset("Referência: 24 h", weeks.map { 24 }, Brand.RED, type = "line", extra = js("({ borderDash: [6, 4], pointRadius: 0, borderWidth: 1.5 })")),
                            Charts.dataset("Ainda aguardando", weeks.map { it.stillPending }, "rgba(217,119,6,.35)", extra = js("({ yAxisID: 'y1' })")),
                        ),
                        yTitle = "horas",
                    ).also { config ->
                        config.asDynamic().options.scales.y1 = js("({ position: 'right', beginAtZero: true, grid: { display: false }, title: { display: true, text: 'agendamentos' } })")
                    }
                }
            }
        }
        // Question 3
        div {
            className = ClassName("col-12 col-xl-6")
            card(
                title = "3. Em que dias e turnos mais perdemos horários com faltas?",
                question = "Faltas ÷ consultas com comparecimento registrado (C07), por dia da semana e turno da consulta.",
            ) {
                val days = (1..5).toList()
                fun share(day: Int, shift: Shift): Double? =
                    data.noShowByWeekdayShift.firstOrNull { it.dayOfWeek == day && it.shift == shift }
                        ?.let { if (it.marked == 0) null else it.noShows * 100.0 / it.marked }
                ChartView {
                    height = 300
                    description = "Taxa de faltas por dia da semana e turno"
                    config = Charts.config(
                        type = "bar",
                        labels = days.map { Format.WEEKDAYS[it - 1] },
                        datasets = listOf(
                            Charts.dataset("Manhã", days.map { share(it, Shift.Morning) }, Brand.PURPLE),
                            Charts.dataset("Tarde", days.map { share(it, Shift.Afternoon) }, Brand.TEAL),
                        ),
                        yTitle = "% de faltas",
                    )
                }
                div {
                    className = ClassName("small text-secondary mt-2")
                    +"Cobertura do registro de comparecimento: ${Format.percent(data.attendanceCoverage)} (${Format.ratio(data.attendanceCoverage)} consultas passadas)."
                }
            }
        }
    }

    card(
        title = "Evolução diária",
        question = "Agendamentos recebidos por dia de agendamento (C01) e faltas por dia de consulta (C07).",
    ) {
        ChartView {
            height = 260
            description = "Agendamentos recebidos e faltas por dia"
            config = Charts.config(
                type = "line",
                labels = data.daily.map { Format.shortDate(it.date) },
                datasets = listOf(
                    Charts.dataset("Agendamentos recebidos", data.daily.map { it.received }, Brand.BLUE, extra = js("({ fill: true, backgroundColor: 'rgba(45,125,210,.12)' })")),
                    Charts.dataset("Faltas", data.daily.map { it.noShows }, Brand.PURPLE),
                    Charts.dataset("Compareceram", data.daily.map { it.attended }, Brand.GREEN),
                ),
            )
        }
    }

    div {
        className = ClassName("row")
        div {
            className = ClassName("col-12 col-xl-7")
            card(title = "Profissionais", question = "Ocupação, faltas e tempo de resposta de cada profissional no período.", bodyClass = "card-body p-0") {
                professionalsTable(data)
            }
        }
        div {
            className = ClassName("col-12 col-xl-5")
            card(title = "Horários de pico (C09)", question = "Consultas marcadas por dia da semana e horário.") {
                peakTable(data)
            }
        }
    }

    limitsBox(demoNote)
}

private fun ChildrenBuilder.kpiRow(data: ClinicDashboard, pendingHref: String?) {
    div {
        className = ClassName("row g-3 mb-4")
        fun col(block: ChildrenBuilder.() -> Unit) = div { className = ClassName("col-12 col-sm-6 col-xl-4 col-xxl-2"); block() }
        col {
            kpiBox(
                "C01", "Agendamentos", Format.integer(data.received), "vs. período anterior", "bi-calendar-plus", Tone.Blue,
                "Agendamentos feitos pelos pacientes no período (data do agendamento), em qualquer situação.",
                trend = Format.variation(data.received, data.receivedPrevious),
            )
        }
        col {
            kpiBox(
                "C02", "Ocupação", Format.percent(data.occupancy), Format.ratio(data.occupancy) + " horários", "bi-grid-3x3-gap", Tone.Teal,
                "Horários ocupados ÷ horários oferecidos (16 por dia útil por profissional ativo), por data da consulta.",
            )
        }
        col {
            kpiBox(
                "C03", "Confirmação", Format.percent(data.confirmation), Format.ratio(data.confirmation) + " respondidos", "bi-check2-circle", Tone.Green,
                "Confirmados ou remarcados ÷ agendamentos que a clínica já respondeu (primeira resposta).",
            )
        }
        col {
            kpiBox(
                "C04", "Tempo até responder", Format.hours(data.medianConfirmationHours), "mediana do período", "bi-stopwatch", Tone.Amber,
                "Mediana das horas entre o agendamento e a primeira resposta da clínica. Horas corridas.",
                alert = (data.medianConfirmationHours ?: 0.0) > 24,
            )
        }
        col {
            div {
                if (pendingHref != null) {
                    a {
                        className = ClassName("text-decoration-none")
                        href = pendingHref
                        pendingBox(data)
                    }
                } else {
                    pendingBox(data)
                }
            }
        }
        col {
            kpiBox(
                "C07", "Faltas", Format.percent(data.noShow), Format.ratio(data.noShow) + " registradas", "bi-person-x", Tone.Purple,
                "Faltas ÷ consultas com comparecimento registrado. Depende de a recepção registrar.",
            )
        }
    }
}

private fun ChildrenBuilder.pendingBox(data: ClinicDashboard) {
    kpiBox(
        "C05", "Pendentes > 24 h", Format.integer(data.pendingOver24h),
        if (data.pendingOver24h > 0) "pacientes esperando resposta" else "nenhum paciente esperando",
        "bi-hourglass-split", Tone.Red,
        "Agendamentos ainda aguardando, feitos há mais de 24 h, com consulta futura. Não segue o filtro de período.",
        alert = data.pendingOver24h > 0,
    )
}

/** Specialty × consultation day, colored by occupancy. */
private fun ChildrenBuilder.occupancyHeatmap(data: ClinicDashboard) {
    val names = data.specialties.associate { it.specialtyId to it.name }
    val dates = data.occupancyBySpecialtyDay.map { it.date }.distinct().sorted()
    val cells = data.occupancyBySpecialtyDay.associateBy { it.specialtyId to it.date }
    div {
        className = ClassName("table-responsive")
        table {
            className = ClassName("heat-table")
            thead {
                tr {
                    th { }
                    dates.forEach { date ->
                        th {
                            key = Key(date.toString())
                            title = Format.weekdayDate(date)
                            +date.day.toString()
                            if (date.day == 1 || date == dates.first()) {
                                div { className = ClassName("fw-normal"); +Format.shortDate(date).substringAfter('/') }
                            }
                        }
                    }
                }
            }
            tbody {
                data.specialties.sortedBy { it.name }.forEach { specialty ->
                    tr {
                        key = Key(specialty.specialtyId.toString())
                        th { +(names[specialty.specialtyId] ?: "") }
                        dates.forEach { date ->
                            val cell = cells[specialty.specialtyId to date]
                            val share = if (cell == null || cell.offered == 0) null else cell.taken.toDouble() / cell.offered
                            td {
                                key = Key(date.toString())
                                title = "${specialty.name}, ${Format.weekdayDate(date)}: " +
                                    (if (cell == null) "sem oferta" else "${cell.taken} de ${cell.offered} horários (${Format.percent(share)})")
                                style = js("({})").unsafeCast<react.CSSProperties>().also {
                                    it.asDynamic().background = heatColor(share)
                                    it.asDynamic().color = if ((share ?: 0.0) > 0.62) "#fff" else Brand.INK
                                }
                                +(share?.let { (it * 100).toInt().toString() } ?: "")
                            }
                        }
                    }
                }
            }
        }
    }
    div {
        className = ClassName("d-flex align-items-center gap-2 small text-secondary mt-2")
        +"Ocupação:"
        listOf(0.1, 0.4, 0.7, 0.95).forEach { v ->
            span {
                className = ClassName("d-inline-block rounded")
                style = js("({ width: '22px', height: '12px' })").unsafeCast<react.CSSProperties>().also { it.asDynamic().background = heatColor(v) }
            }
            +"${(v * 100).toInt()}%"
        }
        span { className = ClassName("ms-2"); +"Números = % ocupado. Passe o mouse para ver os horários." }
    }
}

private fun heatColor(share: Double?): String {
    if (share == null) return "#f1f5f9"
    val alpha = 0.08 + 0.87 * share.coerceIn(0.0, 1.0)
    return "rgba(27, 94, 168, $alpha)"
}

private fun ChildrenBuilder.professionalsTable(data: ClinicDashboard) {
    if (data.professionals.isEmpty()) {
        emptyState("bi-person", "Nenhum profissional no filtro.")
        return
    }
    div {
        className = ClassName("table-responsive")
        table {
            className = ClassName("table table-hover align-middle mb-0")
            thead {
                tr {
                    th { +"Profissional" }
                    th { className = ClassName("text-end"); +"Ocupação" }
                    th { className = ClassName("text-end"); +"Faltas" }
                    th { className = ClassName("text-end"); +"Resposta" }
                }
            }
            tbody {
                data.professionals.sortedByDescending { it.occupancy.value ?: -1.0 }.forEach { professional ->
                    tr {
                        key = Key(professional.professionalId.toString())
                        td {
                            strong { +professional.name }
                            div { className = ClassName("small text-secondary"); +professional.specialtyName }
                        }
                        td { className = ClassName("text-end"); rateCell(professional.occupancy) }
                        td { className = ClassName("text-end"); rateCell(professional.noShow) }
                        td { className = ClassName("text-end"); +Format.hours(professional.medianConfirmationHours) }
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.rateCell(rate: Rate) {
    +Format.percent(rate)
    div { className = ClassName("small text-secondary"); +Format.ratio(rate) }
}

/** C09: weekday × time, as a small heat table. */
private fun ChildrenBuilder.peakTable(data: ClinicDashboard) {
    if (data.peak.isEmpty()) {
        emptyState("bi-clock", "Sem consultas no período.")
        return
    }
    val times = data.peak.map { it.time }.distinct().sorted()
    val max = data.peak.maxOf { it.count }.coerceAtLeast(1)
    val counts = data.peak.associate { (it.dayOfWeek to it.time) to it.count }
    div {
        className = ClassName("table-responsive")
        table {
            className = ClassName("heat-table w-100")
            thead {
                tr {
                    th { }
                    (1..5).forEach { day -> th { key = Key(day.toString()); +Format.weekday(DayOfWeek(day)) } }
                }
            }
            tbody {
                times.forEach { time ->
                    tr {
                        key = Key(time.toString())
                        th { +Format.time(time) }
                        (1..5).forEach { day ->
                            val count = counts[day to time] ?: 0
                            td {
                                key = Key(day.toString())
                                title = "${Format.weekday(DayOfWeek(day))} ${Format.time(time)}: $count consultas"
                                style = js("({})").unsafeCast<react.CSSProperties>().also {
                                    it.asDynamic().background = heatColor(count.toDouble() / max)
                                    it.asDynamic().color = if (count.toDouble() / max > 0.62) "#fff" else Brand.INK
                                }
                                +count.toString()
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.limitsBox(demoNote: Boolean) {
    div {
        className = ClassName("limits mb-4")
        strong { +"Limites dos dados" }
        ul {
            className = ClassName("mb-0 mt-1")
            if (demoNote) li { +"Ambiente de demonstração: os dados são fictícios (base sintética com semente fixa), com padrões inseridos de propósito." }
            li { +"A ocupação mede a demanda atendida: quem não achou vaga e desistiu não deixa registro." }
            li { +"Enquanto a grade de horários for fixa (16 por dia útil), um profissional de meio período aparece com ocupação menor que a real." }
            li { +"O tempo até a resposta conta horas corridas, inclusive noites e fins de semana, e não inclui quem ainda não foi respondido." }
            li { +"A taxa de faltas depende de a recepção registrar o comparecimento; veja a cobertura no gráfico 3." }
        }
    }
}

@Suppress("unused")
private fun LocalDate.weekdayNumber(): Int = dayOfWeek.isoDayNumber
