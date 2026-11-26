package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.admin.Page
import grupotp.medcall.admin.Router
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.kpi.IntegrationMode
import kotlinx.coroutines.launch
import react.ChildrenBuilder
import react.FC
import react.Key
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
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

internal val ClinicList = FC {
    val session = useSession()
    var clinics by useState<List<ManagedClinic>?>(null)
    var error by useState<String?>(null)
    var reload by useState(0)
    var editing by useState<ManagedClinic?>(null)
    var creating by useState(false)

    useEffect(reload) {
        try {
            clinics = session.api.clinics()
            error = null
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }
    errorAlert(error) { reload++ }
    card(
        title = "Clínicas parceiras",
        question = "Clínica nova começa como rascunho e só aparece no app quando ativa (com profissional e administrador).",
        tools = {
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary btn-sm")
                onClick = { creating = true }
                i { className = ClassName("bi bi-plus-lg me-1") }
                +"Nova clínica"
            }
        },
        bodyClass = "card-body p-0",
    ) {
        val list = clinics
        when {
            list == null -> loading()
            list.isEmpty() -> emptyState("bi-hospital", "Nenhuma clínica ainda.")
            else -> div {
                className = ClassName("table-responsive")
                table {
                    className = ClassName("table table-hover align-middle mb-0")
                    thead {
                        tr {
                            th { +"Clínica" }
                            th { +"Situação" }
                            th { className = ClassName("d-none d-md-table-cell"); +"Fuso" }
                            th { className = ClassName("text-end"); +"Profissionais" }
                            th { className = ClassName("text-end"); +"Administradores" }
                            th { className = ClassName("d-none d-lg-table-cell"); +"Último agendamento" }
                            th { className = ClassName("text-end"); +"Ações" }
                        }
                    }
                    tbody {
                        list.forEach { clinic ->
                            tr {
                                key = Key(clinic.id.toString())
                                td {
                                    a { className = ClassName("fw-semibold"); href = Router.href(Page.MedcallClinics, clinic.id.toString()); +clinic.name }
                                    div { className = ClassName("small text-secondary"); +listOf(clinic.city, clinic.address).filter { it.isNotBlank() }.joinToString(" · ") }
                                }
                                td {
                                    statusPill(clinic.status)
                                    span { className = ClassName("badge text-bg-light border ms-1"); +(if (clinic.integrationMode == IntegrationMode.Webhook) "webhook" else "manual") }
                                }
                                td { className = ClassName("d-none d-md-table-cell small"); +clinic.timeZoneId }
                                td { className = ClassName("text-end"); +clinic.professionals.toString() }
                                td { className = ClassName("text-end${if (clinic.admins == 0) " text-danger" else ""}"); +clinic.admins.toString() }
                                td { className = ClassName("d-none d-lg-table-cell small text-secondary"); +(clinic.lastAppointmentAt?.let { Format.dateTime(it) } ?: "—") }
                                td {
                                    className = ClassName("text-end text-nowrap")
                                    button {
                                        type = Kinds.button
                                        className = ClassName("btn btn-sm btn-outline-secondary")
                                        onClick = { editing = clinic }
                                        i { className = ClassName("bi bi-pencil") }
                                        span { className = ClassName("d-none d-xl-inline ms-1"); +"Editar" }
                                    }
                                    statusButton(clinic) { status ->
                                        appScope.launch {
                                            try {
                                                session.api.setClinicStatus(clinic.id, status)
                                                session.notify("Situação de ${clinic.name} atualizada.", false)
                                            } catch (failure: ApiException) {
                                                session.notify(failure.message, true)
                                            }
                                            reload++
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
    if (creating || editing != null) {
        ClinicForm {
            clinic = editing
            onClose = { creating = false; editing = null }
            onSaved = { creating = false; editing = null; reload++ }
        }
    }
}


private fun ChildrenBuilder.statusButton(clinic: ManagedClinic, onChange: (ClinicStatus) -> Unit) {
    val (target, text, css) = when (clinic.status) {
        ClinicStatus.Active -> Triple(ClinicStatus.Inactive, "Desativar", "btn-outline-danger")
        else -> Triple(ClinicStatus.Active, "Ativar", "btn-outline-success")
    }
    button {
        type = Kinds.button
        className = ClassName("btn btn-sm $css ms-1")
        onClick = { onChange(target) }
        i { className = ClassName("bi ${if (target == ClinicStatus.Active) "bi-toggle-on" else "bi-toggle-off"}") }
        span { className = ClassName("d-none d-xl-inline ms-1"); +text }
    }
}

