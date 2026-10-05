package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.AuthResult
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SignIn
import grupotp.recifesaudavel.app.usecase.SignInCommand
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.p
import react.useState

internal external interface LoginScreenProps : Props {
    var signIn: SignIn
    var requestPasswordRecovery: RequestPasswordRecovery
    var onRegister: () -> Unit
    var onBack: () -> Unit
}

internal val LoginScreen = FC<LoginScreenProps> { props ->
    var email by useState("")
    var password by useState("")
    var showPassword by useState(false)
    var submissionError by useState<String?>(null)

    fun clearError() {
        submissionError = null
    }

    div {
        className = classNames("screen")
        screenHeader(title = "Entrar", onBack = props.onBack)
        form {
            className = classNames("screen-scroll")
            onSubmit = { event ->
                event.preventDefault()
                val result = props.signIn(SignInCommand(email = email, password = password))
                submissionError = result.errorMessage()
            }
            div {
                className = classNames("screen-content")
                div {
                    className = classNames("card")
                    formField(caption = "E-mail", required = true) {
                        textInput(
                            id = "login-email",
                            value = email,
                            placeholder = "seu@email.com",
                            type = emailKind,
                            onValue = {
                                email = it
                                clearError()
                            },
                        )
                    }
                    formField(caption = "Senha", required = true, hint = "Mínimo 6 caracteres") {
                        div {
                            className = classNames("password-field")
                            textInput(
                                id = "login-password",
                                value = password,
                                placeholder = "••••••••",
                                type = if (showPassword) textKind else passwordKind,
                                extraClass = "with-icon",
                                onValue = {
                                    password = it
                                    clearError()
                                },
                            )
                            button {
                                type = buttonKind
                                className = classNames("icon-button")
                                ariaLabel = if (showPassword) "Ocultar senha" else "Mostrar senha"
                                onClick = { showPassword = !showPassword }
                                if (showPassword) eyeOffIcon() else eyeIcon()
                            }
                        }
                    }
                    submissionError?.let { message ->
                        errorAlert(message)
                    }
                    button {
                        type = buttonKind
                        className = classNames("text-link")
                        onClick = {
                            val result = props.requestPasswordRecovery(email)
                            submissionError = result.errorMessage()
                        }
                        +"Esqueci minha senha"
                    }
                }
                div {
                    className = classNames("form-actions")
                    button {
                        type = submitKind
                        className = classNames("btn-primary")
                        +"Entrar"
                    }
                    button {
                        type = buttonKind
                        className = classNames("text-link text-link-center")
                        onClick = { props.onRegister() }
                        +"Ainda não tenho conta — Criar cadastro"
                    }
                }
            }
        }
        appFooter()
    }
}

private fun AuthResult.errorMessage(): String? = when (this) {
    AuthResult.Success -> null
    is AuthResult.Failed -> error.toMessage()
}
