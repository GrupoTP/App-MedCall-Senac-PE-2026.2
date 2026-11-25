package grupotp.medcall.app.ui.screen.auth

import grupotp.medcall.app.di.useInject
import grupotp.medcall.app.domain.auth.AuthResult
import grupotp.medcall.app.domain.auth.RecoveryResult
import grupotp.medcall.app.ui.component.alert.errorAlert
import grupotp.medcall.app.ui.component.button.primaryButton
import grupotp.medcall.app.ui.component.button.textLink
import grupotp.medcall.app.ui.component.form.FieldKind
import grupotp.medcall.app.ui.component.form.TextField
import grupotp.medcall.app.ui.component.form.formField
import grupotp.medcall.app.ui.component.layout.AppFooter
import grupotp.medcall.app.ui.component.layout.ScreenHeader
import grupotp.medcall.app.ui.format.ErrorMessages
import grupotp.medcall.app.ui.hook.useScreenScope
import grupotp.medcall.app.ui.icon.eyeIcon
import grupotp.medcall.app.ui.icon.eyeOffIcon
import grupotp.medcall.app.ui.theme.Palette
import grupotp.medcall.app.ui.theme.Styles
import grupotp.medcall.app.usecase.auth.ConfirmPasswordRecoveryUseCase
import grupotp.medcall.app.usecase.auth.RequestPasswordRecoveryUseCase
import grupotp.medcall.app.usecase.auth.SignInUseCase
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.auth.ConfirmPasswordRecoveryRequest
import grupotp.medcall.common.auth.SignInRequest
import grupotp.medcall.common.security.SecretString
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Props
import react.useState

private const val LOGIN = "login"
private const val CODE = "code"
private const val DONE = "done"

internal external interface LoginScreenProps : Props {
    var onRegister: () -> Unit
    var onBack: () -> Unit
    var onAuthenticated: (AuthenticatedAccount) -> Unit
}

internal val LoginScreen = FC<LoginScreenProps> { props ->
    val signIn = useInject<SignInUseCase>()
    val requestPasswordRecovery = useInject<RequestPasswordRecoveryUseCase>()
    val confirmPasswordRecovery = useInject<ConfirmPasswordRecoveryUseCase>()
    var step by useState(LOGIN)
    var email by useState("")
    var password by useState("")
    var code by useState("")
    var newPassword by useState("")
    var confirmPassword by useState("")
    var showPassword by useState(false)
    var submissionError by useState<String?>(null)
    var notice by useState<String?>(null)
    val scope = useScreenScope()

    fun clearError() {
        submissionError = null
    }

    fun submit() {
        scope.launch {
            val result = signIn(SignInRequest(email = email, password = SecretString(password)))
            when (result) {
                is AuthResult.Success -> props.onAuthenticated(result.account)
                is AuthResult.Failed -> submissionError = ErrorMessages.of(result.error)
            }
        }
    }

    fun askForCode() {
        scope.launch {
            when (val result = requestPasswordRecovery(email)) {
                RecoveryResult.Accepted -> {
                    notice = "Se este e-mail estiver cadastrado, enviamos um código. Ele vale por 30 minutos e só pode ser usado uma vez."
                    submissionError = null
                    step = CODE
                }
                is RecoveryResult.Failed -> submissionError = ErrorMessages.of(result.error)
            }
        }
    }

    fun saveNewPassword() {
        if (newPassword != confirmPassword) {
            submissionError = "A confirmação da senha não confere."
            return
        }
        scope.launch {
            val result = confirmPasswordRecovery(
                ConfirmPasswordRecoveryRequest(
                    email = email,
                    code = SecretString(code.trim()),
                    newPassword = SecretString(newPassword),
                ),
            )
            when (result) {
                RecoveryResult.Accepted -> {
                    password = ""
                    code = ""
                    newPassword = ""
                    confirmPassword = ""
                    notice = "Senha alterada. Entre com a nova senha."
                    submissionError = null
                    step = DONE
                }
                is RecoveryResult.Failed -> submissionError = ErrorMessages.of(result.error)
            }
        }
    }

    View {
        style = Styles.screen
        ScreenHeader {
            title = if (step == LOGIN) "Entrar" else "Nova senha"
            onBack = if (step == LOGIN) props.onBack else ({ step = LOGIN })
        }
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.scrollContent
            keyboardShouldPersistTaps = "handled"
            View {
                style = Styles.card
                if (step == LOGIN) {
                    formField(caption = "E-mail", required = true) {
                        TextField {
                            label = "E-mail"
                            value = email
                            placeholder = "seu@email.com"
                            kind = FieldKind.Email
                            onValue = {
                                email = it
                                clearError()
                            }
                        }
                    }
                    formField(caption = "Senha", required = true, hint = "Mínimo 6 caracteres") {
                        View {
                            style = Styles.passwordField
                            TextField {
                                label = "Senha"
                                value = password
                                placeholder = "••••••••"
                                kind = FieldKind.Password
                                obscured = !showPassword
                                withIcon = true
                                onValue = {
                                    password = it
                                    clearError()
                                }
                                onSubmit = ::submit
                            }
                            Pressable {
                                style = Styles.passwordToggle
                                accessibilityRole = "button"
                                accessibilityLabel = if (showPassword) "Ocultar senha" else "Mostrar senha"
                                onPress = { showPassword = !showPassword }
                                if (showPassword) eyeOffIcon(Palette.MUTED) else eyeIcon(Palette.MUTED)
                            }
                        }
                    }
                } else {
                    notice?.let { message ->
                        Text {
                            style = Styles.fieldHint
                            +message
                        }
                    }
                    if (step == CODE) {
                        formField(caption = "Código", required = true) {
                            TextField {
                                label = "Código"
                                value = code
                                placeholder = "Código recebido por e-mail"
                                kind = FieldKind.Text
                                onValue = {
                                    code = it
                                    clearError()
                                }
                            }
                        }
                        formField(caption = "Nova senha", required = true, hint = "Mínimo 6 caracteres") {
                            TextField {
                                label = "Nova senha"
                                value = newPassword
                                placeholder = "••••••••"
                                kind = FieldKind.Password
                                obscured = !showPassword
                                onValue = {
                                    newPassword = it
                                    clearError()
                                }
                            }
                        }
                        formField(caption = "Confirmar nova senha", required = true) {
                            TextField {
                                label = "Confirmar nova senha"
                                value = confirmPassword
                                placeholder = "••••••••"
                                kind = FieldKind.Password
                                obscured = !showPassword
                                onValue = {
                                    confirmPassword = it
                                    clearError()
                                }
                            }
                        }
                    }
                }
                submissionError?.let { message ->
                    errorAlert(message)
                }
                if (step == LOGIN) {
                    textLink("Esqueci minha senha") {
                        askForCode()
                    }
                }
            }
            View {
                style = Styles.formActions
                when (step) {
                    LOGIN -> {
                        primaryButton("Entrar", onPress = ::submit)
                        textLink("Ainda não tenho conta — Criar cadastro", centered = true) {
                            props.onRegister()
                        }
                    }
                    CODE -> primaryButton("Salvar nova senha", onPress = ::saveNewPassword)
                    else -> primaryButton("Voltar para entrar", onPress = { step = LOGIN })
                }
            }
        }
        AppFooter()
    }
}
