package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.pages.field
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.admin.SaveClinicRequest
import kotlinx.coroutines.launch
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.useEffectOnce
import react.useState
import web.cssom.ClassName
import web.dom.ElementId

external interface ClinicFormProps : Props {
    var clinic: ManagedClinic?
    var onClose: () -> Unit
    var onSaved: () -> Unit
}


internal val ClinicForm = FC<ClinicFormProps> { props ->
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

