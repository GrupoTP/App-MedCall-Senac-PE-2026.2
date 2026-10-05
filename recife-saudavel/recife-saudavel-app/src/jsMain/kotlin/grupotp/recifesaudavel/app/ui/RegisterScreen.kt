package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.AuthResult
import grupotp.recifesaudavel.app.usecase.RegisterAccount
import grupotp.recifesaudavel.app.usecase.RegisterAccountCommand
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.useState

internal external interface RegisterScreenProps : Props {
    var registerAccount: RegisterAccount
    var onBack: () -> Unit
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

    fun update(next: RegisterDraft) {
        draft = next
        submissionError = null
    }

    div {
        className = classNames("screen")
        screenHeader(title = "Criar cadastro", onBack = props.onBack)
        form {
            className = classNames("screen-scroll")
            onSubmit = { event ->
                event.preventDefault()
                val result = props.registerAccount(draft.toCommand())
                submissionError = when (result) {
                    AuthResult.Success -> null
                    is AuthResult.Failed -> result.error.toMessage()
                }
            }
            div {
                className = classNames("screen-content")
                div {
                    className = classNames("card")
                    sectionTitle("Identificação")
                    formField(caption = "Nome completo", required = true) {
                        textInput(
                            id = "register-name",
                            value = draft.fullName,
                            placeholder = "Ex: Maria das Graças Silva",
                            onValue = { update(draft.copy(fullName = it)) },
                        )
                    }
                    formField(
                        caption = "Nome Social",
                        hint = "Como prefere ser chamado(a). Editável nas configurações.",
                    ) {
                        textInput(
                            id = "register-social-name",
                            value = draft.socialName,
                            placeholder = "Ex: Graça",
                            onValue = { update(draft.copy(socialName = it)) },
                        )
                    }
                    formField(caption = "E-mail", required = true) {
                        textInput(
                            id = "register-email",
                            value = draft.email,
                            placeholder = "seu@email.com",
                            type = emailKind,
                            onValue = { update(draft.copy(email = it)) },
                        )
                    }
                    formField(caption = "Senha", required = true, hint = "Mínimo 6 caracteres") {
                        textInput(
                            id = "register-password",
                            value = draft.password,
                            placeholder = "••••••••",
                            type = passwordKind,
                            onValue = { update(draft.copy(password = it)) },
                        )
                    }
                }
                div {
                    className = classNames("card")
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
                div {
                    className = classNames("card")
                    sectionTitle("Contato de emergência")
                    formField(caption = "Nome do contato") {
                        textInput(
                            id = "register-emergency-name",
                            value = draft.emergencyContactName,
                            placeholder = "Ex: Ana Silva (filha)",
                            onValue = { update(draft.copy(emergencyContactName = it)) },
                        )
                    }
                    formField(caption = "Telefone do contato") {
                        textInput(
                            id = "register-emergency-phone",
                            value = draft.emergencyContactPhone,
                            placeholder = "(81) 99888-7766",
                            type = telKind,
                            onValue = { update(draft.copy(emergencyContactPhone = it)) },
                        )
                    }
                }
                div {
                    className = classNames("card")
                    sectionTitle("Consentimentos")
                    p {
                        className = classNames("field-hint")
                        +"Escolhas independentes. Leia cada item antes de marcar."
                    }
                    for (option in consentOptions) {
                        label {
                            className = classNames(
                                "consent",
                                if (option.selected(draft)) "consent-selected" else null,
                            )
                            input {
                                type = checkboxKind
                                checked = option.selected(draft)
                                onChange = { event ->
                                    update(option.update(draft, event.target.checked))
                                }
                            }
                            span {
                                className = classNames("consent-copy")
                                +option.caption
                                if (option.required) {
                                    span {
                                        className = classNames("required")
                                        +"*"
                                    }
                                }
                            }
                        }
                    }
                }
                p {
                    className = classNames("required-note")
                    span {
                        className = classNames("required")
                        +"*"
                    }
                    +" Campos e consentimentos obrigatórios"
                }
                submissionError?.let { message ->
                    errorAlert(message)
                }
                button {
                    type = submitKind
                    className = classNames("btn-primary")
                    +"Criar conta"
                }
            }
        }
        appFooter()
    }
}

private fun RegisterDraft.toCommand(): RegisterAccountCommand = RegisterAccountCommand(
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
