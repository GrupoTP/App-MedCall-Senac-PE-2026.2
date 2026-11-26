package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.Session
import grupotp.medcall.admin.api.AdminApi
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.pages.field
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.admin.ui.secretOnce
import grupotp.medcall.common.integration.ApiKey
import grupotp.medcall.common.integration.DeliveryStatus
import grupotp.medcall.common.integration.SaveWebhookRequest
import grupotp.medcall.common.integration.Webhook
import grupotp.medcall.common.integration.WebhookDelivery
import grupotp.medcall.common.integration.WebhookEvent
import grupotp.medcall.common.integration.WebhookTestResult
import grupotp.medcall.common.integration.WebhookWithSecret
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
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ol
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
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.dom.ElementId
import web.navigator.navigator

/**
 * The webhook actions of one portal: the clinic admin's own, or the Painel MedCall's, which
 * reaches every clinic. The screens are the same; only the routes and the clinic column change.
 */
class WebhookAccess(
    val list: suspend () -> List<Webhook>,
    val deliveries: suspend () -> List<WebhookDelivery>,
    /** The clinic is null in the clinic portal (the server uses the admin's own). */
    val create: suspend (clinicId: Int?, request: SaveWebhookRequest) -> WebhookWithSecret,
    val update: suspend (id: Int, request: SaveWebhookRequest) -> Webhook,
    val rotateSecret: suspend (id: Int) -> WebhookWithSecret,
    val test: suspend (id: Int) -> WebhookTestResult,
    val redeliver: suspend (id: Int) -> WebhookDelivery,
) {
    companion object {
        fun clinic(api: AdminApi) = WebhookAccess(
            list = { api.webhooks() },
            deliveries = { api.deliveries(null) },
            create = { _, request -> api.createWebhook(request) },
            update = api::updateWebhook,
            rotateSecret = api::rotateWebhookSecret,
            test = api::testWebhook,
            redeliver = api::redeliver,
        )

        fun medcall(api: AdminApi, clinicFilter: Int?) = WebhookAccess(
            list = { api.medcallWebhooks(clinicFilter) },
            deliveries = { api.medcallDeliveries(clinicFilter) },
            create = { clinicId, request -> api.medcallCreateWebhook(checkNotNull(clinicId), request) },
            update = api::medcallUpdateWebhook,
            rotateSecret = api::medcallRotateWebhookSecret,
            test = api::medcallTestWebhook,
            redeliver = api::medcallRedeliver,
        )
    }
}

/** Portal da Clínica: webhooks, delivery log and API keys (SPM-97 to SPM-100). */
val ClinicIntegrationsPage = FC {
    val session = useSession()
    var keys by useState<List<ApiKey>?>(null)
    var error by useState<String?>(null)
    var reload by useState(0)
    var secret by useState<Pair<String, String>?>(null)
    var newKeyName by useState("")

    useEffect(reload) {
        try {
            keys = session.api.apiKeys()
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

    div {
        className = ClassName("alert alert-light border d-flex gap-3 align-items-start mb-4")
        i { className = ClassName("bi bi-diagram-3 fs-3 text-primary") }
        div {
            strong { +"Como funciona" }
            ol {
                className = ClassName("mb-1 mt-1 small")
                li { +"Cada novo agendamento entra numa fila interna do MedCall (RabbitMQ, quando configurado no servidor)." }
                li { +"O MedCall envia um POST assinado (HMAC-SHA256) para a URL do seu sistema; se falhar, tenta de novo por até 9 horas." }
                li { +"Seu sistema responde pela API pública com a chave de API: confirmar, recusar, remarcar ou registrar comparecimento." }
            }
            a {
                className = ClassName("small")
                href = AdminApi.apiBase() + "/api/public/docs"
                asDynamic().target = "_blank"
                +"Documentação da API pública (OpenAPI)"
            }
        }
    }
    errorAlert(error) { reload++ }

    WebhooksSection {
        access = WebhookAccess.clinic(session.api)
        clinics = null
        clinicFilter = null
    }

    card(
        title = "Chaves de API",
        question = "Autorizam o sistema da clínica na API pública. Guardamos só um resumo da chave: se perder, gere outra.",
        bodyClass = "card-body p-0",
    ) {
        div {
            className = ClassName("p-3 d-flex gap-2 flex-wrap")
            input {
                className = ClassName("form-control")
                style = js("({ maxWidth: '320px' })").unsafeCast<react.CSSProperties>()
                placeholder = "Nome da chave (ex.: ERP produção)"
                asDynamic()["aria-label"] = "Nome da nova chave"
                value = newKeyName
                onChange = { event -> newKeyName = event.target.value }
            }
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary")
                disabled = newKeyName.isBlank()
                onClick = {
                    run(null) {
                        val created = session.api.createApiKey(newKeyName.trim())
                        newKeyName = ""
                        secret = "Chave de API “${created.apiKey.name}”" to created.key.value
                    }
                }
                i { className = ClassName("bi bi-key me-1") }
                +"Gerar chave"
            }
        }
        val list = keys
        when {
            list == null -> loading()
            list.isEmpty() -> emptyState("bi-key", "Nenhuma chave criada.")
            else -> div {
                className = ClassName("table-responsive")
                table {
                    className = ClassName("table align-middle mb-0")
                    thead { tr { th { +"Nome" }; th { +"Final" }; th { +"Criada" }; th { +"Último uso" }; th { className = ClassName("text-end"); +"" } } }
                    tbody {
                        list.forEach { key ->
                            tr {
                                this.key = Key(key.id.toString())
                                td { +key.name; div { className = ClassName("small text-secondary"); +"por ${key.createdBy}" } }
                                td { code { +"…${key.lastFour}" } }
                                td { className = ClassName("small"); +Format.dateTime(key.createdAt) }
                                td { className = ClassName("small"); +(key.lastUsedAt?.let { Format.dateTime(it) } ?: "nunca") }
                                td {
                                    className = ClassName("text-end")
                                    if (key.revokedAt != null) {
                                        span { className = ClassName("badge text-bg-secondary"); +"revogada" }
                                    } else {
                                        button {
                                            type = Kinds.button
                                            className = ClassName("btn btn-sm btn-outline-danger")
                                            onClick = { run("Chave revogada. A próxima chamada com ela será recusada.") { session.api.revokeApiKey(key.id) } }
                                            +"Revogar"
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

    secretModal(session, secret) { secret = null }
}

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

/** Shows a secret once, with a copy button. */
private fun ChildrenBuilder.secretModal(session: Session, shown: Pair<String, String>?, onClose: () -> Unit) {
    if (shown == null) return
    modal("Guarde este valor agora", onClose, footer = {
        button { type = Kinds.button; className = ClassName("btn btn-primary"); onClick = { onClose() }; +"Já guardei" }
    }) {
        secretOnce(shown.first, shown.second) {
            navigator.clipboard.writeTextAsync(shown.second)
            session.notify("Copiado.", false)
        }
    }
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

external interface WebhookFormProps : Props {
    var access: WebhookAccess
    var webhook: Webhook?
    /** Clinics to pick from (Painel MedCall); null in the clinic portal. */
    var clinics: List<Pair<Int, String>>?
    var defaultClinic: Int?
    var onClose: () -> Unit
    /** Receives the new secret when the webhook was just created. */
    var onSaved: (String?) -> Unit
}

private val WebhookForm = FC<WebhookFormProps> { props ->
    val session = useSession()
    val existing = props.webhook
    val clinics = props.clinics
    var clinicId by useState(existing?.clinicId ?: props.defaultClinic ?: clinics?.firstOrNull()?.first)
    var url by useState(existing?.url ?: "https://")
    var description by useState(existing?.description ?: "")
    var events by useState(existing?.events?.toSet() ?: setOf(WebhookEvent.AppointmentCreated, WebhookEvent.AppointmentCancelledByPatient))
    var active by useState(existing?.active ?: true)
    var busy by useState(false)
    var error by useState<String?>(null)
    modal(
        if (existing == null) "Novo webhook" else "Editar webhook",
        props.onClose,
        footer = {
            button { type = Kinds.button; className = ClassName("btn btn-outline-secondary"); onClick = { props.onClose() }; +"Cancelar" }
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary")
                disabled = busy || !url.startsWith("http") || events.isEmpty() || (clinics != null && clinicId == null)
                onClick = {
                    busy = true
                    appScope.launch {
                        try {
                            val request = SaveWebhookRequest(url.trim(), description.trim(), events.toList(), active)
                            if (existing == null) {
                                props.onSaved(props.access.create(clinicId, request).secret.value)
                            } else {
                                props.access.update(existing.id, request)
                                session.notify("Webhook atualizado.", false)
                                props.onSaved(null)
                            }
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
        if (clinics != null) {
            div {
                className = ClassName("mb-3")
                label { className = ClassName("form-label"); htmlFor = ElementId("hook-clinic"); +"Clínica" }
                select {
                    id = ElementId("hook-clinic")
                    className = ClassName("form-select")
                    // A webhook stays with its clinic; to move it, create one in the other clinic.
                    disabled = existing != null
                    value = clinicId?.toString() ?: ""
                    onChange = { event -> clinicId = event.target.value.toIntOrNull() }
                    clinics.forEach { (value, name) -> option { key = Key(value.toString()); this.value = value.toString(); +name } }
                }
                div { className = ClassName("form-text"); +"A ação fica no Registro de ações da clínica, com o seu nome." }
            }
        }
        field("hook-url", "URL do sistema da clínica (HTTPS)", Kinds.url, url, help = "Endereços internos (localhost, IPs privados) são recusados.") { url = it }
        field("hook-description", "Descrição", Kinds.text, description, required = false) { description = it }
        div { className = ClassName("form-label"); +"Eventos" }
        WebhookEvent.entries.forEach { event ->
            div {
                key = Key(event.name)
                className = ClassName("form-check")
                input {
                    id = ElementId("event-${event.name}")
                    type = Kinds.checkbox
                    className = ClassName("form-check-input")
                    checked = event in events
                    onChange = { change -> events = if (change.target.checked) events + event else events - event }
                }
                label {
                    className = ClassName("form-check-label")
                    htmlFor = ElementId("event-${event.name}")
                    +event.label
                    code { className = ClassName("ms-2 small"); +event.wireName }
                }
            }
        }
        div {
            className = ClassName("form-check form-switch mt-3")
            input {
                id = ElementId("hook-active")
                type = Kinds.checkbox
                className = ClassName("form-check-input")
                checked = active
                onChange = { change -> active = change.target.checked }
            }
            label { className = ClassName("form-check-label"); htmlFor = ElementId("hook-active"); +"Ativo" }
        }
        if (existing == null) p {
            className = ClassName("small text-secondary mt-3 mb-0")
            +(if (clinics == null) "O segredo de assinatura aparece uma única vez, depois de salvar."
            else "O segredo de assinatura aparece uma única vez, depois de salvar: entregue-o à clínica por um canal seguro.")
        }
    }
}
