package grupotp.medcall.admin.pages

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.admin.ui.secretOnce
import grupotp.medcall.common.staff.CreateStaffUserRequest
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.common.staff.StaffUserWithPassword
import kotlinx.coroutines.launch
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.select
import react.useState
import web.cssom.ClassName
import web.dom.ElementId
import web.navigator.navigator

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

