package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.pages.field
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.admin.ManagedProfessional
import grupotp.medcall.common.admin.SaveProfessionalRequest
import grupotp.medcall.common.scheduling.Specialty
import kotlinx.coroutines.launch
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.useState
import web.cssom.ClassName
import web.dom.ElementId

external interface ProfessionalFormProps : Props {
    var professional: ManagedProfessional?
    var clinics: List<ManagedClinic>
    var specialties: List<Specialty>
    var defaultClinic: Int?
    var onClose: () -> Unit
    var onSaved: () -> Unit
}


internal val ProfessionalForm = FC<ProfessionalFormProps> { props ->
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

