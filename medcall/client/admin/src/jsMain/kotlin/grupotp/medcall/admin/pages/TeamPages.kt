package grupotp.medcall.admin.pages

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.admin.ui.secretOnce
import grupotp.medcall.common.admin.AuditEntry
import grupotp.medcall.common.staff.CreateStaffUserRequest
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.common.staff.StaffUserWithPassword
import grupotp.medcall.common.staff.UpdateStaffUserRequest
import grupotp.medcall.common.staff.StaffAccount
import kotlinx.coroutines.launch
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.label
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

/** What the team table can do; null hides the action. */
external interface StaffTableProps : Props {
    var users: List<StaffUser>
    var me: StaffAccount
    var clinicNames: Map<Int, String>?
    var onRole: ((StaffUser, StaffRole) -> Unit)?
    var onActive: ((StaffUser, Boolean) -> Unit)?
    var onReset: ((StaffUser) -> Unit)?
}

val StaffTable = FC<StaffTableProps> { props ->
    if (props.users.isEmpty()) {
        emptyState("bi-people", "Nenhum usuário ainda.")
        return@FC
    }
    div {
        className = ClassName("table-responsive")
        table {
            className = ClassName("table table-hover align-middle mb-0")
            thead {
                tr {
                    th { +"Nome" }
                    if (props.clinicNames != null) th { +"Clínica" }
                    th { +"Perfil" }
                    th { +"Situação" }
                    th { className = ClassName("d-none d-lg-table-cell"); +"Último acesso" }
                    th { className = ClassName("text-end"); +"Ações" }
                }
            }
            tbody {
                props.users.forEach { user ->
                    val isMe = user.id == props.me.id
                    tr {
                        key = Key(user.id.toString())
                        td {
                            strong { +user.fullName }
                            if (isMe) span { className = ClassName("badge text-bg-light border ms-2"); +"você" }
                            div { className = ClassName("small text-secondary"); +user.email }
                        }
                        val names = props.clinicNames
                        if (names != null) td { +(user.clinicId?.let { names[it] } ?: "—") }
                        td {
                            val onRole = props.onRole
                            if (onRole != null && !isMe) {
                                select {
                                    className = ClassName("form-select form-select-sm")
                                    asDynamic()["aria-label"] = "Perfil de ${user.fullName}"
                                    value = user.role.name
                                    onChange = { event -> onRole(user, StaffRole.valueOf(event.target.value)) }
                                    listOf(StaffRole.ClinicAdmin, StaffRole.Receptionist).forEach {
                                        option { key = Key(it.name); value = it.name; +Format.role(it) }
                                    }
                                }
                            } else {
                                +Format.role(user.role)
                            }
                        }
                        td {
                            span {
                                className = ClassName("badge ${if (user.active) "text-bg-success" else "text-bg-secondary"}")
                                +(if (user.active) "Ativo" else "Inativo")
                            }
                            if (user.mustChangePassword && user.active) {
                                div { className = ClassName("small text-secondary"); +"senha provisória" }
                            }
                        }
                        td {
                            className = ClassName("d-none d-lg-table-cell small text-secondary")
                            +(user.lastLoginAt?.let { Format.dateTime(it) } ?: "nunca entrou")
                        }
                        td {
                            className = ClassName("text-end text-nowrap")
                            val onReset = props.onReset
                            if (onReset != null && !isMe) {
                                button {
                                    type = Kinds.button
                                    className = ClassName("btn btn-sm btn-outline-secondary ms-1")
                                    title = "Gerar nova senha provisória"
                                    onClick = { onReset(user) }
                                    i { className = ClassName("bi bi-key") }
                                    span { className = ClassName("d-none d-xl-inline ms-1"); +"Nova senha" }
                                }
                            }
                            val onActive = props.onActive
                            if (onActive != null && !isMe) {
                                button {
                                    type = Kinds.button
                                    className = ClassName("btn btn-sm ${if (user.active) "btn-outline-danger" else "btn-outline-success"} ms-1")
                                    onClick = { onActive(user, !user.active) }
                                    i { className = ClassName("bi ${if (user.active) "bi-person-dash" else "bi-person-check"}") }
                                    span { className = ClassName("d-none d-xl-inline ms-1"); +(if (user.active) "Desativar" else "Reativar") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

external interface NewUserProps : Props {
    var title: String
    var roles: List<StaffRole>
    var onCreate: suspend (CreateStaffUserRequest) -> StaffUserWithPassword
    var onClose: () -> Unit
}

/** Form for a new account; after creating, shows the temporary password once. */
val NewUserDialog = FC<NewUserProps> { props ->
    val session = useSession()
    var name by useState("")
    var email by useState("")
    var role by useState(props.roles.first())
    var busy by useState(false)
    var error by useState<String?>(null)
    var created by useState<StaffUserWithPassword?>(null)

    val done = created
    modal(
        props.title,
        props.onClose,
        footer = {
            if (done == null) {
                button { type = Kinds.button; className = ClassName("btn btn-outline-secondary"); onClick = { props.onClose() }; +"Cancelar" }
                button {
                    type = Kinds.button
                    className = ClassName("btn btn-primary")
                    disabled = busy || name.isBlank() || !email.contains("@")
                    onClick = {
                        busy = true
                        appScope.launch {
                            try {
                                created = props.onCreate(CreateStaffUserRequest(name.trim(), email.trim(), role))
                            } catch (failure: ApiException) {
                                error = failure.message
                            } finally {
                                busy = false
                            }
                        }
                    }
                    +"Criar acesso"
                }
            } else {
                button { type = Kinds.button; className = ClassName("btn btn-primary"); onClick = { props.onClose() }; +"Pronto" }
            }
        },
    ) {
        if (done != null) {
            p { +"Acesso criado para ${done.user.fullName} (${done.user.email})." }
            secretOnce("Senha provisória (a pessoa troca no primeiro acesso)", done.temporaryPassword.value) {
                navigator.clipboard.writeTextAsync(done.temporaryPassword.value)
                session.notify("Senha copiada.", false)
            }
            return@modal
        }
        errorAlert(error)
        field("new-user-name", "Nome completo", Kinds.text, name) { name = it }
        field("new-user-email", "E-mail", Kinds.email, email, help = "É o login. Repasse a senha provisória por um canal seguro.") { email = it }
        if (props.roles.size > 1) {
            label { className = ClassName("form-label"); htmlFor = ElementId("new-user-role"); +"Perfil" }
            select {
                id = ElementId("new-user-role")
                className = ClassName("form-select")
                value = role.name
                onChange = { event -> role = StaffRole.valueOf(event.target.value) }
                props.roles.forEach { option { key = Key(it.name); value = it.name; +Format.role(it) } }
            }
            div {
                className = ClassName("form-text")
                +"Administrador: equipe, indicadores e integrações. Recepcionista: só a agenda."
            }
        }
    }
}

/** Shows a new temporary password after a reset. */
fun ChildrenBuilder.resetResult(result: StaffUserWithPassword?, onCopy: (String) -> Unit, onClose: () -> Unit) {
    if (result == null) return
    modal("Nova senha provisória", onClose, footer = {
        button { type = Kinds.button; className = ClassName("btn btn-primary"); onClick = { onClose() }; +"Pronto" }
    }) {
        p { +"As sessões de ${result.user.fullName} foram encerradas." }
        secretOnce("Senha provisória", result.temporaryPassword.value) { onCopy(result.temporaryPassword.value) }
    }
}

/** Portal da Clínica: the clinic administrator manages the team (SPM-91). */
val ClinicTeamPage = FC {
    val session = useSession()
    var users by useState<List<StaffUser>?>(null)
    var error by useState<String?>(null)
    var reload by useState(0)
    var creating by useState(false)
    var reset by useState<StaffUserWithPassword?>(null)

    useEffect(reload) {
        try {
            users = session.api.clinicUsers()
            error = null
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }

    fun change(user: StaffUser, request: UpdateStaffUserRequest, message: String) {
        appScope.launch {
            try {
                session.api.updateClinicUser(user.id, request)
                session.notify(message, false)
            } catch (failure: ApiException) {
                session.notify(failure.message, true)
            }
            reload++
        }
    }

    errorAlert(error) { reload++ }
    card(
        title = "Usuários do portal",
        question = "A clínica sempre mantém pelo menos um administrador ativo.",
        tools = {
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary btn-sm")
                onClick = { creating = true }
                i { className = ClassName("bi bi-person-plus me-1") }
                +"Novo usuário"
            }
        },
        bodyClass = "card-body p-0",
    ) {
        val list = users
        if (list == null) {
            loading()
        } else {
            StaffTable {
                this.users = list
                me = session.account
                onRole = { user, role -> change(user, UpdateStaffUserRequest(role = role), "Perfil de ${user.fullName} alterado.") }
                onActive = { user, active ->
                    change(user, UpdateStaffUserRequest(active = active), if (active) "${user.fullName} reativado." else "${user.fullName} desativado; as sessões foram encerradas.")
                }
                onReset = { user ->
                    appScope.launch {
                        try {
                            reset = session.api.resetClinicUserPassword(user.id)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        }
                    }
                }
            }
        }
    }
    if (creating) {
        NewUserDialog {
            title = "Novo usuário da clínica"
            roles = listOf(StaffRole.Receptionist, StaffRole.ClinicAdmin)
            onCreate = { request -> session.api.createClinicUser(request).also { reload++ } }
            onClose = { creating = false }
        }
    }
    resetResult(reset, { navigator.clipboard.writeTextAsync(it); session.notify("Senha copiada.", false) }) { reset = null }
}

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
