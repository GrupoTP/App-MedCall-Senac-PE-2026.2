package grupotp.medcall.admin.pages

import grupotp.medcall.admin.api.AdminApi
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.common.staff.StaffAccount
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.small
import react.useState
import web.cssom.ClassName

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

