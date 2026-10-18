package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.AuthResult
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.toMessage
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Key
import react.Props
import react.useState

internal external interface RegisterScreenProps : Props {
    var registerAccount: RegisterAccount
    var onBack: () -> Unit
    var onAuthenticated: (AuthenticatedAccount) -> Unit
}

private data class RegisterDraft(
    val fullName: String = "",
    val socialName: String = "",
    val email: String = "",
    val password: String = "",
    val primaryPhone: String = "",
    val primaryPhoneWhatsapp: Boolean = false,
    val secondaryPhone: String = "",
    val secondaryPhoneWhatsapp: Boolean = false,
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val acceptedPrivacyPolicy: Boolean = false,
    val acceptedTreatment: Boolean = false,
    val wantsNotifications: Boolean = false,
)

private data class ConsentOption(
    val caption: String,
    val required: Boolean,
    val selected: (RegisterDraft) -> Boolean,
    val update: (RegisterDraft, Boolean) -> RegisterDraft,
)

private val consentOptions = listOf(
    ConsentOption(
        caption = "Li e estou ciente da Política de Privacidade e do tratamento de dados pelo Recife Saudável.",
        required = true,
        selected = { it.acceptedPrivacyPolicy },
        update = { draft, value -> draft.copy(acceptedPrivacyPolicy = value) },
    ),
    ConsentOption(
        caption = "Concordo com o tratamento dos dados necessário para agendamento de consultas.",
        required = true,
        selected = { it.acceptedTreatment },
        update = { draft, value -> draft.copy(acceptedTreatment = value) },
    ),
    ConsentOption(
        caption = "Desejo receber comunicações e lembretes de consulta (opcional).",
        required = false,
        selected = { it.wantsNotifications },
        update = { draft, value -> draft.copy(wantsNotifications = value) },
    ),
)

internal val RegisterScreen = FC<RegisterScreenProps> { props ->
    var draft by useState(RegisterDraft())
    var submissionError by useState<String?>(null)
    val scope = useScreenScope()

    fun update(next: RegisterDraft) {
        draft = next
        submissionError = null
    }

    View {
        style = Styles.screen
        ScreenHeader {
            title = "Criar cadastro"
            onBack = props.onBack
        }
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.scrollContent
            keyboardShouldPersistTaps = "handled"
            View {
                style = Styles.card
                sectionTitle("Identificação")
                formField(caption = "Nome completo", required = true) {
                    TextField {
                        label = "Nome completo"
                        value = draft.fullName
                        placeholder = "Ex: Maria das Graças Silva"
                        kind = FieldKind.Name
                        onValue = { update(draft.copy(fullName = it)) }
                    }
                }
                formField(
                    caption = "Nome Social",
                    hint = "Como prefere ser chamado(a). Editável nas configurações.",
                ) {
                    TextField {
                        label = "Nome Social"
                        value = draft.socialName
                        placeholder = "Ex: Graça"
                        kind = FieldKind.Name
                        onValue = { update(draft.copy(socialName = it)) }
                    }
                }
                formField(caption = "E-mail", required = true) {
                    TextField {
                        label = "E-mail"
                        value = draft.email
                        placeholder = "seu@email.com"
                        kind = FieldKind.Email
                        onValue = { update(draft.copy(email = it)) }
                    }
                }
                formField(caption = "Senha", required = true, hint = "Mínimo 6 caracteres") {
                    TextField {
                        label = "Senha"
                        value = draft.password
                        placeholder = "••••••••"
                        kind = FieldKind.NewPassword
                        onValue = { update(draft.copy(password = it)) }
                    }
                }
            }
            View {
                style = Styles.card
                sectionTitle("Contato")
                phoneField(
                    caption = "Telefone principal",
                    value = draft.primaryPhone,
                    whatsapp = draft.primaryPhoneWhatsapp,
                    required = true,
                    onValue = { update(draft.copy(primaryPhone = it)) },
                    onWhatsapp = { update(draft.copy(primaryPhoneWhatsapp = it)) },
                )
                phoneField(
                    caption = "Telefone alternativo",
                    value = draft.secondaryPhone,
                    whatsapp = draft.secondaryPhoneWhatsapp,
                    onValue = { update(draft.copy(secondaryPhone = it)) },
                    onWhatsapp = { update(draft.copy(secondaryPhoneWhatsapp = it)) },
                )
            }
            View {
                style = Styles.card
                sectionTitle("Contato de emergência")
                formField(caption = "Nome do contato") {
                    TextField {
                        label = "Nome do contato"
                        value = draft.emergencyContactName
                        placeholder = "Ex: Ana Silva (filha)"
                        kind = FieldKind.Name
                        onValue = { update(draft.copy(emergencyContactName = it)) }
                    }
                }
                formField(caption = "Telefone do contato") {
                    TextField {
                        label = "Telefone do contato"
                        value = draft.emergencyContactPhone
                        placeholder = "(81) 99888-7766"
                        kind = FieldKind.Phone
                        onValue = { update(draft.copy(emergencyContactPhone = it)) }
                    }
                }
            }
            View {
                style = Styles.card
                sectionTitle("Consentimentos")
                Text {
                    style = Styles.fieldHint
                    +"Escolhas independentes. Leia cada item antes de marcar."
                }
                for (option in consentOptions) {
                    val selected = option.selected(draft)
                    CheckBox {
                        key = Key(option.caption)
                        checked = selected
                        label = option.caption
                        style = arrayOf(Styles.consent, if (selected) Styles.consentSelected else null)
                        boxStyle = Styles.consentCheckbox
                        onToggle = { update(option.update(draft, it)) }
                        Text {
                            style = Styles.consentCopy
                            +option.caption
                            if (option.required) {
                                Text {
                                    style = Styles.required
                                    +" *"
                                }
                            }
                        }
                    }
                }
            }
            Text {
                style = Styles.requiredNote
                Text {
                    style = Styles.required
                    +"*"
                }
                +" Campos e consentimentos obrigatórios"
            }
            submissionError?.let { message ->
                errorAlert(message)
            }
            primaryButton("Criar conta") {
                scope.launch {
                    val result = props.registerAccount(draft.toRequest())
                    when (result) {
                        is AuthResult.Success -> props.onAuthenticated(result.account)
                        is AuthResult.Failed -> submissionError = result.error.toMessage()
                    }
                }
            }
        }
        AppFooter()
    }
}

private fun RegisterDraft.toRequest(): RegisterAccountRequest = RegisterAccountRequest(
    fullName = fullName,
    socialName = socialName,
    email = email,
    password = password,
    primaryPhone = primaryPhone,
    primaryPhoneWhatsapp = primaryPhoneWhatsapp,
    secondaryPhone = secondaryPhone,
    secondaryPhoneWhatsapp = secondaryPhoneWhatsapp,
    emergencyContactName = emergencyContactName,
    emergencyContactPhone = emergencyContactPhone,
    acceptedPrivacyPolicy = acceptedPrivacyPolicy,
    acceptedTreatment = acceptedTreatment,
    wantsNotifications = wantsNotifications,
)
