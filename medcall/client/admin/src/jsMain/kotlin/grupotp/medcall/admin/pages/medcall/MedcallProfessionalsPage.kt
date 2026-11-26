package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.idFilter
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.admin.ManagedProfessional
import grupotp.medcall.common.scheduling.Specialty
import kotlinx.coroutines.launch
import react.FC
import react.Key
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
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

val MedcallProfessionalsPage = FC {
    val session = useSession()
    var clinics by useState<List<ManagedClinic>>(emptyList())
    var specialties by useState<List<Specialty>>(emptyList())
    var clinicFilter by useState<Int?>(null)
    var professionals by useState<List<ManagedProfessional>?>(null)
    var reload by useState(0)
    var editing by useState<ManagedProfessional?>(null)
    var creating by useState(false)
    var newSpecialty by useState("")

    useEffect(reload) {
        clinics = runCatching { session.api.clinics() }.getOrDefault(emptyList())
        specialties = runCatching { session.api.specialties() }.getOrDefault(emptyList())
    }
    useEffect(clinicFilter, reload) {
        professionals = runCatching { session.api.professionals(clinicFilter) }.getOrNull()
    }
    val clinicNames = clinics.associate { it.id to it.name }

    div {
        className = ClassName("row")
        div {
            className = ClassName("col-12 col-xl-8")
            card(
                title = "Profissionais",
                question = "Profissional inativo deixa de oferecer horários no app; as consultas já marcadas continuam.",
                tools = {
                    button {
                        type = Kinds.button
                        className = ClassName("btn btn-primary btn-sm")
                        disabled = clinics.isEmpty() || specialties.isEmpty()
                        onClick = { creating = true }
                        i { className = ClassName("bi bi-plus-lg me-1") }
                        +"Novo profissional"
                    }
                },
                bodyClass = "card-body p-0",
            ) {
                div {
                    className = ClassName("p-3 pb-0 row")
                    idFilter("prof-clinic", "Clínica", "Todas", clinics.map { it.id to it.name }, clinicFilter, width = "col-12 col-md-6") { clinicFilter = it }
                }
                val list = professionals
                when {
                    list == null -> loading()
                    list.isEmpty() -> emptyState("bi-person-badge", "Nenhum profissional.")
                    else -> div {
                        className = ClassName("table-responsive mt-2")
                        table {
                            className = ClassName("table table-hover align-middle mb-0")
                            thead { tr { th { +"Profissional" }; th { +"Especialidade" }; th { +"Clínica" }; th { +"Situação" }; th { className = ClassName("text-end"); +"" } } }
                            tbody {
                                list.forEach { professional ->
                                    tr {
                                        key = Key(professional.id.toString())
                                        td {
                                            strong { +professional.name }
                                            if (professional.registration.isNotBlank()) div { className = ClassName("small text-secondary"); +professional.registration }
                                        }
                                        td { +professional.specialtyName }
                                        td { +(clinicNames[professional.clinicId] ?: "—") }
                                        td {
                                            span {
                                                className = ClassName("badge ${if (professional.active) "text-bg-success" else "text-bg-secondary"}")
                                                +(if (professional.active) "Ativo" else "Inativo")
                                            }
                                        }
                                        td {
                                            className = ClassName("text-end")
                                            button {
                                                type = Kinds.button
                                                className = ClassName("btn btn-sm btn-outline-secondary")
                                                onClick = { editing = professional }
                                                i { className = ClassName("bi bi-pencil") }
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
        div {
            className = ClassName("col-12 col-xl-4")
            card(title = "Especialidades", question = "Lista única para todas as clínicas.") {
                div {
                    className = ClassName("d-flex flex-wrap gap-1 mb-3")
                    specialties.forEach { span { key = Key(it.id.toString()); className = ClassName("badge text-bg-light border fw-normal"); +it.name } }
                }
                div {
                    className = ClassName("input-group")
                    input {
                        className = ClassName("form-control")
                        placeholder = "Nova especialidade"
                        asDynamic()["aria-label"] = "Nova especialidade"
                        value = newSpecialty
                        onChange = { event -> newSpecialty = event.target.value }
                    }
                    button {
                        type = Kinds.button
                        className = ClassName("btn btn-outline-primary")
                        disabled = newSpecialty.isBlank()
                        onClick = {
                            appScope.launch {
                                try {
                                    session.api.createSpecialty(newSpecialty.trim())
                                    session.notify("Especialidade criada.", false)
                                    newSpecialty = ""
                                    reload++
                                } catch (failure: ApiException) {
                                    session.notify(failure.message, true)
                                }
                            }
                        }
                        +"Adicionar"
                    }
                }
            }
        }
    }
    if (creating || editing != null) {
        ProfessionalForm {
            professional = editing
            this.clinics = clinics
            this.specialties = specialties
            defaultClinic = clinicFilter
            onClose = { creating = false; editing = null }
            onSaved = { creating = false; editing = null; reload++ }
        }
    }
}

