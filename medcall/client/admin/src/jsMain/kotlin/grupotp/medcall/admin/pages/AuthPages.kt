package grupotp.medcall.admin.pages

import grupotp.medcall.admin.api.AdminApi
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.common.staff.StaffAccount
import kotlinx.coroutines.launch
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.span
import react.useState
import web.cssom.ClassName
import web.dom.ElementId
import web.html.InputType

external interface LoginProps : Props {
    var api: AdminApi
    var onSignedIn: (StaffAccount) -> Unit
}

/** One sign-in for both portals: the account's role decides where it lands. */
val LoginPage = FC<LoginProps> { props ->
    var email by useState("")
    var password by useState("")
    var error by useState<String?>(null)
    var busy by useState(false)

    div {
        className = ClassName("login-page")
        div {
            className = ClassName("card login-card shadow-sm")
            div {
                className = ClassName("card-body p-4 p-md-5")
                div {
                    className = ClassName("text-center mb-4")
                    img { className = ClassName("logo mb-3"); src = "medcall-symbol.svg"; alt = "MedCall" }
                    h1 { className = ClassName("h4 mb-1"); +"Portais MedCall" }
                    p { className = ClassName("text-secondary mb-0"); +"Portal da Clínica e Painel de Controle" }
                }
                errorAlert(error)
                form {
                    noValidate = false
                    onSubmit = { event ->
                        event.preventDefault()
                        busy = true
                        error = null
                        appScope.launch {
                            try {
                                props.onSignedIn(props.api.signIn(email.trim(), password))
                            } catch (failure: ApiException) {
                                error = failure.message
                            } finally {
                                busy = false
                            }
                        }
                    }
                    field("login-email", "E-mail", Kinds.email, email, "username") { email = it }
                    field("login-password", "Senha", Kinds.password, password, "current-password") { password = it }
                    button {
                        type = Kinds.submit
                        className = ClassName("btn btn-primary w-100 mt-2")
                        disabled = busy || email.isBlank() || password.isBlank()
                        if (busy) span { className = ClassName("spinner-border spinner-border-sm me-2") }
                        +"Entrar"
                    }
                }
                small {
                    className = ClassName("d-block text-secondary text-center mt-4")
                    +"Pacientes usam o app MedCall. Este acesso é das clínicas parceiras e da equipe MedCall."
                }
            }
        }
    }
}

external interface ChangePasswordProps : Props {
    var api: AdminApi
    var account: StaffAccount
    var onChanged: (StaffAccount) -> Unit
    var onSignOut: () -> Unit
}

/** First access with a temporary password: nothing else opens until it is changed. */
val ChangePasswordPage = FC<ChangePasswordProps> { props ->
    var current by useState("")
    var next by useState("")
    var repeat by useState("")
    var error by useState<String?>(null)
    var busy by useState(false)
    val mismatch = repeat.isNotEmpty() && next != repeat

    div {
        className = ClassName("login-page")
        div {
            className = ClassName("card login-card shadow-sm")
            div {
                className = ClassName("card-body p-4 p-md-5")
                h1 { className = ClassName("h4 mb-1"); +"Crie sua senha" }
                p {
                    className = ClassName("text-secondary")
                    +"Olá, ${props.account.fullName}. Você entrou com uma senha provisória (${Format.role(props.account.role)}"
                    props.account.clinicName?.let { +" · $it" }
                    +"). Escolha uma senha só sua, com pelo menos 8 caracteres."
                }
                errorAlert(error)
                form {
                    onSubmit = { event ->
                        event.preventDefault()
                        busy = true
                        error = null
                        appScope.launch {
                            try {
                                props.onChanged(props.api.changePassword(current, next))
                            } catch (failure: ApiException) {
                                error = failure.message
                            } finally {
                                busy = false
                            }
                        }
                    }
                    field("pw-current", "Senha provisória", Kinds.password, current, "current-password") { current = it }
                    field("pw-new", "Nova senha", Kinds.password, next, "new-password") { next = it }
                    field("pw-repeat", "Repita a nova senha", Kinds.password, repeat, "new-password") { repeat = it }
                    if (mismatch) div { className = ClassName("text-danger small mb-2"); +"As senhas não são iguais." }
                    button {
                        type = Kinds.submit
                        className = ClassName("btn btn-primary w-100")
                        disabled = busy || current.isEmpty() || next.length < 8 || next != repeat
                        +"Salvar e continuar"
                    }
                    button {
                        type = Kinds.button
                        className = ClassName("btn btn-link w-100 mt-2")
                        onClick = { props.onSignOut() }
                        +"Sair"
                    }
                }
            }
        }
    }
}

/** A labeled Bootstrap input. */
fun ChildrenBuilder.field(
    id: String,
    label: String,
    type: InputType,
    value: String,
    autoComplete: String? = null,
    help: String? = null,
    required: Boolean = true,
    onChange: (String) -> Unit,
) {
    div {
        className = ClassName("mb-3")
        label { className = ClassName("form-label"); htmlFor = ElementId(id); +label }
        input {
            this.id = ElementId(id)
            this.type = type
            className = ClassName("form-control")
            this.value = value
            this.required = required
            if (autoComplete != null) asDynamic().autoComplete = autoComplete
            this.onChange = { event -> onChange(event.target.value) }
        }
        if (help != null) div { className = ClassName("form-text"); +help }
    }
}
