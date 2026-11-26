package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.common.integration.DeliveryStatus
import grupotp.medcall.common.integration.Webhook
import grupotp.medcall.common.integration.WebhookDelivery
import kotlinx.coroutines.launch
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.code
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.useEffect
import react.useState
import web.cssom.ClassName

external interface WebhooksSectionProps : Props {
    var access: WebhookAccess
    /** Every clinic (id to name) in the Painel MedCall; null in the clinic portal. */
    var clinics: List<Pair<Int, String>>?
    /** The clinic picked in the Painel MedCall filter; new webhooks start on it. */
    var clinicFilter: Int?
}


/** The "Webhooks" and "Entregas recentes" cards, with the form and the one-time secret. */
val WebhooksSection = FC<WebhooksSectionProps> { props ->
    val session = useSession()
    val access = props.access
    val clinics = props.clinics
    val clinicNames = clinics?.toMap()
    var webhooks by useState<List<Webhook>?>(null)
    var deliveries by useState<List<WebhookDelivery>?>(null)
    var error by useState<String?>(null)
    var reload by useState(0)
    var editing by useState<Webhook?>(null)
    var creating by useState(false)
    var secret by useState<Pair<String, String>?>(null)

    useEffect(reload, props.clinicFilter) {
        webhooks = null
        deliveries = null
        try {
            webhooks = access.list()
            deliveries = access.deliveries()
            error = null
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }

    fun run(success: String?, block: suspend () -> Unit) {
        appScope.launch {
            try {
                block()
                if (success != null) session.notify(success, false)
            } catch (failure: ApiException) {
                session.notify(failure.message, true)
            }
            reload++
        }
    }

    errorAlert(error) { reload++ }

    card(
        title = "Webhooks",
        question = if (clinicNames == null) "Para onde o MedCall envia os eventos de agendamento da sua clínica."
        else "Para onde o MedCall envia os eventos de agendamento de cada clínica.",
        tools = {
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary btn-sm")
                disabled = clinics?.isEmpty() == true
                onClick = { creating = true }
                i { className = ClassName("bi bi-plus-lg me-1") }
                +"Novo webhook"
            }
        },
        bodyClass = "card-body p-0",
    ) {
        val list = webhooks
        when {
            list == null -> loading()
            list.isEmpty() -> emptyState(
                "bi-plug",
                if (clinicNames == null) "Nenhum webhook. Sem webhook, a clínica responde pela Agenda do portal."
                else "Nenhum webhook. Sem webhook, a clínica responde pela Agenda do Portal da Clínica.",
            )
            else -> div {
                className = ClassName("table-responsive")
                table {
                    className = ClassName("table align-middle mb-0")
                    thead {
                        tr {
                            if (clinicNames != null) th { +"Clínica" }
                            th { +"URL" }; th { +"Eventos" }; th { +"Situação" }; th { className = ClassName("text-end"); +"Ações" }
                        }
                    }
                    tbody {
                        list.forEach { hook ->
                            tr {
                                key = Key(hook.id.toString())
                                if (clinicNames != null) td { className = ClassName("text-nowrap"); +(clinicNames[hook.clinicId] ?: "#${hook.clinicId}") }
                                td {
                                    code { className = ClassName("small"); +hook.url }
                                    if (hook.description.isNotBlank()) div { className = ClassName("small text-secondary"); +hook.description }
                                }
                                td { className = ClassName("small"); +hook.events.joinToString(", ") { it.label } }
                                td {
                                    span { className = ClassName("badge ${if (hook.active) "text-bg-success" else "text-bg-secondary"}"); +(if (hook.active) "Ativo" else "Inativo") }
                                    if (hook.disabledReason.isNotBlank()) div { className = ClassName("small text-danger"); +hook.disabledReason }
                                }
                                td {
                                    className = ClassName("text-end text-nowrap")
                                    button {
                                        type = Kinds.button
                                        className = ClassName("btn btn-sm btn-outline-primary ms-1")
                                        onClick = {
                                            appScope.launch {
                                                try {
                                                    val result = access.test(hook.id)
                                                    val code = result.statusCode
                                                    session.notify(
                                                        if (code != null && code in 200..299) "Teste entregue: HTTP $code em ${result.durationMs} ms."
                                                        else "Teste falhou: ${code?.let { "HTTP $it" } ?: result.error} (${result.durationMs} ms).",
                                                        code == null || code !in 200..299,
                                                    )
                                                } catch (failure: ApiException) {
                                                    session.notify(failure.message, true)
                                                }
                                                reload++
                                            }
                                        }
                                        i { className = ClassName("bi bi-send me-1") }
                                        +"Enviar teste"
                                    }
                                    button {
                                        type = Kinds.button
                                        className = ClassName("btn btn-sm btn-outline-secondary ms-1")
                                        title = "Editar"
                                        onClick = { editing = hook }
                                        i { className = ClassName("bi bi-pencil") }
                                    }
                                    button {
                                        type = Kinds.button
                                        className = ClassName("btn btn-sm btn-outline-secondary ms-1")
                                        title = "Gerar novo segredo (o antigo vale por mais 24 h)"
                                        onClick = {
                                            run(null) { secret = "Novo segredo de assinatura" to access.rotateSecret(hook.id).secret.value }
                                        }
                                        i { className = ClassName("bi bi-arrow-repeat") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    card(
        title = "Entregas recentes",
        question = "Cada tentativa de envio. Depois de 6 tentativas, o evento vai para a fila de mortas e pode ser reenviado.",
        bodyClass = "card-body p-0",
    ) {
        val list = deliveries
        val clinicOfHook = webhooks.orEmpty().associate { it.id to it.clinicId }
        when {
            list == null -> loading()
            list.isEmpty() -> emptyState("bi-inbox", "Nenhuma entrega ainda.")
            else -> deliveriesTable(
                list,
                clinicOf = clinicNames?.let { names -> { delivery -> clinicOfHook[delivery.webhookId]?.let { names[it] } ?: "—" } },
            ) { delivery -> run("Evento reenviado para a fila.") { access.redeliver(delivery.id) } }
        }
    }

    if (creating || editing != null) {
        WebhookForm {
            this.access = access
            webhook = editing
            this.clinics = clinics
            defaultClinic = props.clinicFilter
            onClose = { creating = false; editing = null }
            onSaved = { shown ->
                creating = false
                editing = null
                if (shown != null) secret = "Segredo de assinatura do webhook" to shown
                reload++
            }
        }
    }
    secretModal(session, secret) { secret = null }
}


private fun ChildrenBuilder.deliveriesTable(
    list: List<WebhookDelivery>,
    clinicOf: ((WebhookDelivery) -> String)?,
    onRedeliver: (WebhookDelivery) -> Unit,
) {
    div {
        className = ClassName("table-responsive")
        table {
            className = ClassName("table table-sm align-middle mb-0")
            thead {
                tr {
                    th { +"Quando" }
                    if (clinicOf != null) th { +"Clínica" }
                    th { +"Evento" }; th { +"Situação" }; th { +"Tentativas" }; th { +"Última resposta" }; th { +"" }
                }
            }
            tbody {
                list.forEach { delivery ->
                    tr {
                        key = Key(delivery.id.toString())
                        td { className = ClassName("small text-nowrap"); +Format.dateTime(delivery.createdAt) }
                        if (clinicOf != null) td { className = ClassName("small text-nowrap"); +clinicOf(delivery) }
                        td { code { className = ClassName("small"); +delivery.eventType } }
                        td {
                            val (css, text) = when (delivery.status) {
                                DeliveryStatus.Delivered -> "text-bg-success" to "entregue"
                                DeliveryStatus.Pending -> "text-bg-light border" to "na fila"
                                DeliveryStatus.Retrying -> "text-bg-warning" to "nova tentativa"
                                DeliveryStatus.Dead -> "text-bg-danger" to "fila de mortas"
                            }
                            span { className = ClassName("badge $css"); +text }
                            delivery.nextAttemptAt?.let { div { className = ClassName("small text-secondary"); +"próxima: ${Format.dateTime(it)}" } }
                        }
                        td { +delivery.attempts.toString() }
                        td {
                            className = ClassName("small")
                            +(delivery.lastStatusCode?.let { "HTTP $it" } ?: delivery.lastError.ifBlank { "—" })
                            delivery.lastDurationMs?.let { span { className = ClassName("text-secondary"); +" · $it ms" } }
                        }
                        td {
                            className = ClassName("text-end")
                            if (delivery.status == DeliveryStatus.Dead) {
                                button {
                                    type = Kinds.button
                                    className = ClassName("btn btn-sm btn-outline-primary")
                                    onClick = { onRedeliver(delivery) }
                                    +"Reenviar"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

