package grupotp.medcall.app.ui.screen.auth

import grupotp.medcall.app.di.useInject
import grupotp.medcall.app.domain.auth.AuthResult
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
import grupotp.medcall.app.usecase.auth.RequestPasswordRecoveryUseCase
import grupotp.medcall.app.usecase.auth.SignInUseCase
import grupotp.medcall.common.auth.AuthenticatedAccount
import grupotp.medcall.common.auth.SignInRequest
import grupotp.medcall.common.security.SecretString
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Props
import react.useState

internal external interface LoginScreenProps : Props {
    var onRegister: () -> Unit
    var onBack: () -> Unit
    var onAuthenticated: (AuthenticatedAccount) -> Unit
}

internal val LoginScreen = FC<LoginScreenProps> { props ->
    val signIn = useInject<SignInUseCase>()
    val requestPasswordRecovery = useInject<RequestPasswordRecoveryUseCase>()
    var email by useState("")
    var password by useState("")
    var showPassword by useState(false)
    var submissionError by useState<String?>(null)
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

    View {
        style = Styles.screen
        ScreenHeader {
            title = "Entrar"
            onBack = props.onBack
        }
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.scrollContent
            keyboardShouldPersistTaps = "handled"
            View {
                style = Styles.card
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
                submissionError?.let { message ->
                    errorAlert(message)
                }
                textLink("Esqueci minha senha") {
                    scope.launch {
                        val result = requestPasswordRecovery(email)
                        submissionError = when (result) {
                            is AuthResult.Success -> null
                            is AuthResult.Failed -> ErrorMessages.of(result.error)
                        }
                    }
                }
            }
            View {
                style = Styles.formActions
                primaryButton("Entrar", onPress = ::submit)
                textLink("Ainda não tenho conta — Criar cadastro", centered = true) {
                    props.onRegister()
                }
            }
        }
        AppFooter()
    }
}
