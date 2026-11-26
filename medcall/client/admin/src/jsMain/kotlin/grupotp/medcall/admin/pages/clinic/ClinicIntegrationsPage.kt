package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.api.AdminApi
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.common.integration.ApiKey
import kotlinx.coroutines.launch
import react.FC
import react.Key
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.code
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ol
import react.dom.html.ReactHTML.p
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

