package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.pages.field
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.common.integration.SaveWebhookRequest
import grupotp.medcall.common.integration.Webhook
import grupotp.medcall.common.integration.WebhookEvent
import kotlinx.coroutines.launch
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.code
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.select
import react.useState
import web.cssom.ClassName
import web.dom.ElementId

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


internal val WebhookForm = FC<WebhookFormProps> { props ->
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

