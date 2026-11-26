package grupotp.medcall.admin.pages

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.common.admin.AuditEntry
import grupotp.medcall.common.staff.StaffRole
import react.FC
import react.Key
import react.dom.html.ReactHTML.div
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

/** "Registro de ações": the audit log of the clinic, or of the platform for MedCall's team. */
val AuditPage = FC {
    val session = useSession()
    var entries by useState<List<AuditEntry>?>(null)
    var error by useState<String?>(null)
    useEffect(session.account.id) {
        try {
            entries = if (session.account.role == StaffRole.MedcallAdmin) session.api.audit() else session.api.clinicAudit()
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }
    errorAlert(error)
    card(title = "Últimas ações", question = "Guardamos quem fez, o quê e quando; nunca dados de pacientes.", bodyClass = "card-body p-0") {
        val list = entries
        when {
            list == null -> loading()
            list.isEmpty() -> emptyState("bi-journal", "Nenhuma ação registrada ainda.")
            else -> div {
                className = ClassName("table-responsive")
                table {
                    className = ClassName("table table-sm align-middle mb-0")
                    thead { tr { th { +"Quando" }; th { +"Quem" }; th { +"Ação" }; th { +"Sobre" } } }
                    tbody {
                        list.forEachIndexed { index, entry ->
                            tr {
                                key = Key(index.toString())
                                td { className = ClassName("text-nowrap small"); +Format.dateTime(entry.occurredAt) }
                                td { +entry.actorName }
                                td { span { className = ClassName("badge text-bg-light border"); +actionLabel(entry.action) } }
                                td { +entry.target }
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun actionLabel(action: String): String = when {
    action == "staff.created" -> "Usuário criado"
    action == "staff.updated" -> "Usuário alterado"
    action == "staff.password_reset" -> "Senha redefinida"
    action == "staff.deactivated" -> "Usuário desativado"
    action == "staff.reactivated" -> "Usuário reativado"
    action == "clinic.created" -> "Clínica cadastrada"
    action == "clinic.updated" -> "Clínica alterada"
    action.startsWith("clinic.status.") -> "Situação da clínica: " + when (action.removePrefix("clinic.status.")) {
        "active" -> "ativa"; "inactive" -> "inativa"; else -> "rascunho"
    }
    action == "professional.created" -> "Profissional cadastrado"
    action == "professional.updated" -> "Profissional alterado"
    action == "professional.deactivated" -> "Profissional desativado"
    action == "specialty.created" -> "Especialidade criada"
    action.startsWith("webhook.") -> "Webhook: " + action.removePrefix("webhook.")
    action.startsWith("api_key.") -> "Chave de API: " + action.removePrefix("api_key.")
    else -> action
}

