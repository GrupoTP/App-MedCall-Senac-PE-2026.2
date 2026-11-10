package grupotp.medcall.admin.ui

import grupotp.medcall.common.scheduling.AppointmentStatus
import react.ChildrenBuilder
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.span
import web.cssom.ClassName

/** Colors of a [kpiBox], as CSS classes (Theme.kt). */
enum class Tone(val css: String) {
    Blue("kpi-blue"), Green("kpi-green"), Amber("kpi-amber"), Red("kpi-red"), Purple("kpi-purple"), Teal("kpi-teal"),
}

/**
 * One indicator: label with its catalog code, value, and the detail that lets a reviewer redo the
 * math ("12 de 40"). [hint] is the KPI's definition, shown on hover.
 */
fun ChildrenBuilder.kpiBox(
    code: String,
    label: String,
    value: String,
    detail: String,
    icon: String,
    tone: Tone,
    hint: String,
    trend: Pair<String, Boolean>? = null,
    alert: Boolean = false,
) {
    div {
        className = ClassName("kpi-box ${tone.css}${if (alert) " alert-state" else ""}")
        title = hint
        div {
            className = ClassName("kpi-icon")
            i { className = ClassName("bi $icon"); ariaHidden = true }
        }
        div {
            className = ClassName("flex-grow-1 min-w-0")
            div {
                className = ClassName("kpi-label")
                +label
                span { className = ClassName("kpi-code"); +code }
                i { className = ClassName("bi bi-info-circle ms-1"); ariaLabel = hint }
            }
            div { className = ClassName("kpi-value"); +value }
            div {
                className = ClassName("kpi-detail")
                if (trend != null) {
                    span {
                        className = ClassName(if (trend.second) "kpi-trend-up me-1" else "kpi-trend-down me-1")
                        +trend.first
                    }
                }
                +detail
            }
        }
    }
}

/** A card with a title, the question it answers and the body. */
fun ChildrenBuilder.card(
    title: String,
    question: String? = null,
    tools: (ChildrenBuilder.() -> Unit)? = null,
    bodyClass: String = "card-body",
    body: ChildrenBuilder.() -> Unit,
) {
    div {
        className = ClassName("card mb-4")
        div {
            className = ClassName("card-header d-flex align-items-start gap-2")
            div {
                className = ClassName("flex-grow-1")
                h3 { className = ClassName("card-title mb-0 float-none"); +title }
                if (question != null) div { className = ClassName("card-question"); +question }
            }
            if (tools != null) div { className = ClassName("card-tools ms-auto"); tools() }
        }
        div { className = ClassName(bodyClass); body() }
    }
}

fun ChildrenBuilder.errorAlert(message: String?, onRetry: (() -> Unit)? = null) {
    if (message == null) return
    div {
        className = ClassName("alert alert-danger d-flex align-items-center gap-2")
        asDynamic().role = "alert"
        i { className = ClassName("bi bi-exclamation-triangle-fill") }
        div { className = ClassName("flex-grow-1"); +message }
        if (onRetry != null) {
            button {
                type = Kinds.button
                className = ClassName("btn btn-sm btn-outline-danger")
                onClick = { onRetry() }
                +"Tentar de novo"
            }
        }
    }
}

fun ChildrenBuilder.loading(text: String = "Carregando…") {
    div {
        className = ClassName("d-flex align-items-center gap-2 text-secondary py-4 justify-content-center")
        div { className = ClassName("spinner-border spinner-border-sm"); asDynamic().role = "status" }
        +text
    }
}

fun ChildrenBuilder.emptyState(icon: String, text: String) {
    div {
        className = ClassName("empty-state")
        i { className = ClassName("bi $icon") }
        +text
    }
}

fun ChildrenBuilder.statusBadge(status: AppointmentStatus, rescheduled: Boolean = false) {
    span {
        className = ClassName("status-badge ${Format.statusClass(status)}")
        +Format.status(status, rescheduled)
    }
}

/** A Bootstrap modal drawn by React (no Bootstrap JS): always open while rendered. */
fun ChildrenBuilder.modal(
    title: String,
    onClose: () -> Unit,
    size: String = "",
    footer: (ChildrenBuilder.() -> Unit)? = null,
    body: ChildrenBuilder.() -> Unit,
) {
    div {
        className = ClassName("modal d-block")
        asDynamic().role = "dialog"
        tabIndex = -1
        onClick = { event -> if (event.target == event.currentTarget) onClose() }
        div {
            className = ClassName("modal-dialog modal-dialog-scrollable $size")
            div {
                className = ClassName("modal-content")
                div {
                    className = ClassName("modal-header")
                    h5 { className = ClassName("modal-title"); +title }
                    button {
                        type = Kinds.button
                        className = ClassName("btn-close")
                        ariaLabel = "Fechar"
                        onClick = { onClose() }
                    }
                }
                div { className = ClassName("modal-body"); body() }
                if (footer != null) div { className = ClassName("modal-footer"); footer() }
            }
        }
    }
}

/** A password or key shown once, with a copy button. */
fun ChildrenBuilder.secretOnce(label: String, secret: String, onCopy: () -> Unit) {
    p { className = ClassName("mb-1 fw-semibold"); +label }
    div { className = ClassName("secret-box mb-2"); +secret }
    div {
        className = ClassName("d-flex align-items-center gap-2")
        button {
            type = Kinds.button
            className = ClassName("btn btn-sm btn-outline-primary")
            onClick = { onCopy() }
            i { className = ClassName("bi bi-clipboard me-1") }
            +"Copiar"
        }
        small { className = ClassName("text-secondary"); +"Ela não será mostrada de novo." }
    }
}
