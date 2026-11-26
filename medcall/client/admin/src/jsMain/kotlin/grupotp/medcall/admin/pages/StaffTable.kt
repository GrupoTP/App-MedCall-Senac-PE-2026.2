package grupotp.medcall.admin.pages

import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.emptyState
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.common.staff.StaffAccount
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import web.cssom.ClassName

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

