package grupotp.medcall.admin.ui

import grupotp.medcall.admin.api.PeriodChoice
import kotlinx.datetime.LocalDate
import react.ChildrenBuilder
import react.Key
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import web.cssom.ClassName
import web.dom.ElementId

/** "Período" select: last 7, 30 or 90 days, or two dates. */
fun ChildrenBuilder.periodFilter(choice: PeriodChoice, onPick: (PeriodChoice) -> Unit) {
    val custom = choice.from != null
    div {
        className = ClassName("col-12 col-sm-6 col-lg-3")
        label { className = ClassName("form-label"); htmlFor = ElementId("filter-period"); +"Período" }
        select {
            id = ElementId("filter-period")
            className = ClassName("form-select")
            value = if (custom) "custom" else choice.days.toString()
            onChange = { event ->
                val value = event.target.value
                onPick(if (value == "custom") PeriodChoice(null, choice.from ?: defaultFrom(), choice.to ?: defaultTo()) else PeriodChoice(value.toInt()))
            }
            listOf("7" to "Últimos 7 dias", "30" to "Últimos 30 dias", "90" to "Últimos 90 dias", "custom" to "Escolher datas…").forEach { (value, text) ->
                option { this.value = value; +text }
            }
        }
    }
    if (custom) {
        dateInput("filter-from", "De", choice.from!!) { onPick(choice.copy(from = it)) }
        dateInput("filter-to", "Até", choice.to!!) { onPick(choice.copy(to = it)) }
    }
}

private fun ChildrenBuilder.dateInput(id: String, text: String, value: LocalDate, onPick: (LocalDate) -> Unit) {
    div {
        className = ClassName("col-6 col-lg-2")
        label { className = ClassName("form-label"); htmlFor = ElementId(id); +text }
        input {
            this.id = ElementId(id)
            type = Kinds.date
            className = ClassName("form-control")
            this.value = value.toString()
            this.onChange = { event -> runCatching { LocalDate.parse(event.target.value) }.onSuccess(onPick) }
        }
    }
}

/** A select of ids with an "all" option. */
fun ChildrenBuilder.idFilter(
    id: String,
    text: String,
    allText: String,
    options: List<Pair<Int, String>>,
    selected: Int?,
    width: String = "col-12 col-sm-6 col-lg-3",
    onPick: (Int?) -> Unit,
) {
    div {
        className = ClassName(width)
        label { className = ClassName("form-label"); htmlFor = ElementId(id); +text }
        select {
            this.id = ElementId(id)
            className = ClassName("form-select")
            value = selected?.toString() ?: ""
            onChange = { event -> onPick(event.target.value.toIntOrNull()) }
            option { value = ""; +allText }
            options.forEach { (value, label) -> option { key = Key(value.toString()); this.value = value.toString(); +label } }
        }
    }
}

/** Today in the browser's calendar (the person is at the clinic or at MedCall). */
fun localToday(): LocalDate {
    val now: dynamic = js("new Date()")
    return LocalDate(now.getFullYear() as Int, (now.getMonth() as Int) + 1, now.getDate() as Int)
}

private fun defaultTo(): LocalDate = localToday()

private fun defaultFrom(): LocalDate = LocalDate.fromEpochDays(localToday().toEpochDays() - 29)

/** Hands [text] to the browser as a file named [name]. */
fun downloadText(name: String, text: String, mime: String = "text/csv;charset=utf-8") {
    val blob: dynamic = js("new Blob([text], { type: mime })")
    val url = js("URL.createObjectURL(blob)")
    val link: dynamic = js("document.createElement('a')")
    link.href = url
    link.download = name
    js("document.body.appendChild(link)")
    link.click()
    link.remove()
    js("setTimeout(function() { URL.revokeObjectURL(url); }, 1000)")
}
