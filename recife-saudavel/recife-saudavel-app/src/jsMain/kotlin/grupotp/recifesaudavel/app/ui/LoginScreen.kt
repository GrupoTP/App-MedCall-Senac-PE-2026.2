package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.AuthResult
import grupotp.recifesaudavel.app.usecase.RequestPasswordRecovery
import grupotp.recifesaudavel.app.usecase.SignIn
import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.SignInRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.p
import react.useEffectOnce
import react.useRef
import react.useState

internal external interface LoginScreenProps : Props {
    var signIn: SignIn
    var requestPasswordRecovery: RequestPasswordRecovery
    var onRegister: () -> Unit
    var onBack: () -> Unit
    var onAuthenticated: (AuthenticatedAccount) -> Unit
}

internal val LoginScreen = FC<LoginScreenProps> { props ->
    var email by useState("")
    var password by useState("")
    var showPassword by useState(false)
    var submissionError by useState<String?>(null)
    val scopeHolder = useRef<CoroutineScope>(null)
    val scope = scopeHolder.current ?: CoroutineScope(SupervisorJob()).also { scopeHolder.current = it }
    useEffectOnce {
        try {
            awaitCancellation()
        } finally {
            scope.cancel()
        }
    }

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
                scope.launch {
                    val result = props.signIn(SignInRequest(email = email, password = password))
                    when (result) {
                        is AuthResult.Success -> props.onAuthenticated(result.account)
                        is AuthResult.Failed -> submissionError = result.error.toMessage()
                    }
                }
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
                            scope.launch {
                                val result = props.requestPasswordRecovery(email)
                                submissionError = when (result) {
                                    is AuthResult.Success -> null
                                    is AuthResult.Failed -> result.error.toMessage()
                                }
                            }
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

