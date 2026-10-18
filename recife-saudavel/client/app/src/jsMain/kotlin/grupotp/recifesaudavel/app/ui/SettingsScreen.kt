package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.platform.Linking
import grupotp.recifesaudavel.app.usecase.ChangePassword
import grupotp.recifesaudavel.app.usecase.LoadAccountProfile
import grupotp.recifesaudavel.app.usecase.RequestAccountDeletion
import grupotp.recifesaudavel.app.usecase.SaveAccountProfile
import grupotp.recifesaudavel.app.usecase.SchedulingError
import grupotp.recifesaudavel.app.usecase.SchedulingResult
import grupotp.recifesaudavel.app.usecase.UpdateNotificationConsent
import grupotp.recifesaudavel.common.AccountProfile
import grupotp.recifesaudavel.common.AuthenticatedAccount
import grupotp.recifesaudavel.common.Phone
import grupotp.recifesaudavel.common.UpdateProfileRequest
import kotlinx.coroutines.launch
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.useEffectOnce
import react.useState
import kotlin.js.Date

internal enum class SettingsTab(val title: String) {
    Account("Conta"),
    Security("Segurança"),
    Privacy("Privacidade"),
}

private const val LGPD_EMAIL = "recifesaudavel@senac.br"

internal external interface SettingsScreenProps : Props {
    var account: AuthenticatedAccount
    var loadAccountProfile: LoadAccountProfile
    var saveAccountProfile: SaveAccountProfile
    var updateNotificationConsent: UpdateNotificationConsent
    var changePassword: ChangePassword
    var requestAccountDeletion: RequestAccountDeletion
    var tab: SettingsTab
    var onTab: (SettingsTab) -> Unit
    var onBack: () -> Unit
    var onPrivacyPolicy: () -> Unit
    var onAccountChanged: (AuthenticatedAccount) -> Unit
    var onLogout: () -> Unit
    var onSignOut: () -> Unit
}

/**
 * The prototype settings screen: account data, security and privacy. Everything but ending the
 * session needs the server, so offline the screen says so instead of showing stale fields.
 */
internal val SettingsScreen = FC<SettingsScreenProps> { props ->
    var profile by useState<AccountProfile?>(null)
    var form by useState<UpdateProfileRequest?>(null)
    var problem by useState<SchedulingError?>(null)
    var saved by useState(false)
    var busy by useState(false)
    var currentPassword by useState("")
    var newPassword by useState("")
    var confirmPassword by useState("")
    var passwordError by useState<String?>(null)
    var passwordChanged by useState<String?>(null)
    var askingOtherSessions by useState(false)
    var confirmingDeletion by useState(false)
    val scope = useScreenScope()

    fun load() {
        problem = null
        scope.launch {
            when (val result = props.loadAccountProfile(props.account)) {
                is SchedulingResult.Success -> {
                    profile = result.value
                    form = result.value.toForm()
                }
                is SchedulingResult.Failed -> problem = result.error
            }
        }
    }

    useEffectOnce { load() }

    fun selectTab(tab: SettingsTab) {
        problem = null
        saved = false
        passwordError = null
        passwordChanged = null
        confirmingDeletion = false
        props.onTab(tab)
    }

    fun <T> handle(result: SchedulingResult<T>, onSuccess: (T) -> Unit) {
        when (result) {
            is SchedulingResult.Success -> onSuccess(result.value)
            is SchedulingResult.Failed -> problem = result.error
        }
    }

    fun save() {
        val request = form ?: return
        if (busy) {
            return
        }
        busy = true
        problem = null
        saved = false
        scope.launch {
            handle(props.saveAccountProfile(props.account, request)) { result ->
                profile = result.profile
                form = result.profile.toForm()
                saved = true
                props.onAccountChanged(result.account)
            }
            busy = false
        }
    }

    // Checks the form here, then asks about the other sessions before anything is sent.
    fun submitPassword() {
        if (busy) {
            return
        }
        passwordChanged = null
        problem = null
        when {
            currentPassword.isEmpty() || newPassword.isEmpty() -> {
                passwordError = "Informe a senha atual e a nova senha."
                return
            }
            newPassword != confirmPassword -> {
                passwordError = "A confirmação não confere com a nova senha."
                return
            }
        }
        passwordError = null
        askingOtherSessions = true
    }

    fun sendPassword(signOutOtherSessions: Boolean) {
        askingOtherSessions = false
        if (busy) {
            return
        }
        busy = true
        scope.launch {
            val result = props.changePassword(props.account, currentPassword, newPassword, signOutOtherSessions)
            when (result) {
                is SchedulingResult.Success -> {
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    passwordChanged = if (signOutOtherSessions) {
                        "Senha alterada. As sessões nos outros aparelhos foram encerradas."
                    } else {
                        "Senha alterada com sucesso."
                    }
                }
                is SchedulingResult.Failed -> when (val error = result.error) {
                    is SchedulingError.Rejected -> passwordError = error.message
                    else -> problem = error
                }
            }
            busy = false
        }
    }

    fun toggleNotifications(wants: Boolean) {
        if (busy) {
            return
        }
        busy = true
        problem = null
        scope.launch {
            handle(props.updateNotificationConsent(props.account, wants)) { profile = it }
            busy = false
        }
    }

    fun requestDeletion() {
        if (busy) {
            return
        }
        busy = true
        problem = null
        scope.launch {
            // The server ended every session of the account, so this device leaves too.
            handle(props.requestAccountDeletion(props.account)) { props.onLogout() }
            busy = false
        }
    }

    View {
        style = Styles.screen
        ScreenHeader {
            menuSpace = true
            title = "Configurações"
            onBack = props.onBack
        }
        View {
            style = Styles.tabBar
            View {
                style = Styles.tabBarInner
                for (option in SettingsTab.entries) {
                    val selected = option == props.tab
                    Pressable {
                        key = Key(option.name)
                        style = pressable(Styles.tab, if (selected) Styles.tabSelected else null)
                        accessibilityRole = "tab"
                        accessibilityLabel = if (selected) "${option.title}, selecionada" else option.title
                        onPress = { selectTab(option) }
                        Text {
                            style = if (selected) Styles.tabTextSelected else Styles.tabText
                            +option.title
                        }
                    }
                }
            }
        }
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.scrollContent
            keyboardShouldPersistTaps = "handled"
            problem?.let { error ->
                sessionExpiredAlert(error, props.onSignOut)
                if (profile == null && error != SchedulingError.SessionExpired) {
                    textLink("Tentar de novo") { load() }
                }
            }
            val loaded = profile
            val editable = form
            when (props.tab) {
                SettingsTab.Account -> if (loaded != null && editable != null) {
                    accountTab(
                        profile = loaded,
                        form = editable,
                        saved = saved,
                        busy = busy,
                        onForm = {
                            form = it
                            saved = false
                        },
                        onSave = ::save,
                    )
                } else if (problem == null) {
                    loadingText()
                }
                SettingsTab.Security -> securityTab(
                    profile = loaded,
                    busy = busy,
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    passwordError = passwordError,
                    passwordChanged = passwordChanged,
                    confirmingDeletion = confirmingDeletion,
                    onCurrent = {
                        currentPassword = it
                        passwordError = null
                    },
                    onNew = {
                        newPassword = it
                        passwordError = null
                    },
                    onConfirm = {
                        confirmPassword = it
                        passwordError = null
                    },
                    onSubmitPassword = ::submitPassword,
                    onLogout = props.onLogout,
                    onAskDeletion = { confirmingDeletion = true },
                    onKeepAccount = { confirmingDeletion = false },
                    onConfirmDeletion = ::requestDeletion,
                )
                SettingsTab.Privacy -> if (loaded != null) {
                    privacyTab(
                        profile = loaded,
                        busy = busy,
                        onNotifications = ::toggleNotifications,
                        onPrivacyPolicy = props.onPrivacyPolicy,
                    )
                } else if (problem == null) {
                    loadingText()
                }
            }
        }
        AppFooter()
        confirmDialog(
            visible = askingOtherSessions,
            title = "Encerrar outras sessões?",
            message = "Se você entrou no Recife Saudável em outro aparelho, podemos encerrar essas sessões " +
                "junto com a troca de senha. Este aparelho continua conectado.",
            confirmText = "Sim, encerrar",
            declineText = "Não, manter",
            onConfirm = { sendPassword(signOutOtherSessions = true) },
            onDecline = { sendPassword(signOutOtherSessions = false) },
            onDismiss = { askingOtherSessions = false },
        )
    }
}

private fun ChildrenBuilder.loadingText() {
    Text {
        style = Styles.loadingText
        accessibilityRole = "progressbar"
        +"Carregando dados…"
    }
}

private fun ChildrenBuilder.accountTab(
    profile: AccountProfile,
    form: UpdateProfileRequest,
    saved: Boolean,
    busy: Boolean,
    onForm: (UpdateProfileRequest) -> Unit,
    onSave: () -> Unit,
) {
    View {
        style = Styles.card
        sectionTitle("Registro da plataforma")
        View {
            style = Styles.summaryListLoose
            summaryLine("Data de criação", formatStamp(profile.createdAt))
            summaryLine("Último login", formatStamp(profile.lastLoginAt), last = true)
        }
    }
    View {
        style = Styles.card
        sectionTitle("Identidade")
        formField(caption = "Nome completo", required = true) {
            TextField {
                label = "Nome completo"
                value = form.fullName
                placeholder = "Ex: Maria das Graças Silva"
                kind = FieldKind.Name
                onValue = { onForm(form.copy(fullName = it)) }
            }
        }
        formField(caption = "Nome Social", hint = "Como prefere ser chamado(a) na plataforma") {
            TextField {
                label = "Nome Social"
                value = form.socialName
                placeholder = "Ex: Graça"
                kind = FieldKind.Name
                onValue = { onForm(form.copy(socialName = it)) }
            }
        }
        formField(caption = "E-mail") {
            TextField {
                label = "E-mail"
                value = form.email
                placeholder = "seu@email.com"
                kind = FieldKind.Email
                onValue = { onForm(form.copy(email = it)) }
            }
        }
    }
    View {
        style = Styles.card
        sectionTitle("Telefones")
        phoneField(
            caption = "Telefone principal",
            value = form.primaryPhone,
            whatsapp = form.primaryPhoneWhatsapp,
            required = true,
            onValue = { onForm(form.copy(primaryPhone = it)) },
            onWhatsapp = { onForm(form.copy(primaryPhoneWhatsapp = it)) },
        )
        phoneField(
            caption = "Telefone alternativo",
            value = form.secondaryPhone,
            whatsapp = form.secondaryPhoneWhatsapp,
            onValue = { onForm(form.copy(secondaryPhone = it)) },
            onWhatsapp = { onForm(form.copy(secondaryPhoneWhatsapp = it)) },
        )
    }
    View {
        style = Styles.card
        sectionTitle("Contato de emergência")
        formField(caption = "Nome do contato") {
            TextField {
                label = "Nome do contato"
                value = form.emergencyContactName
                placeholder = "Ex: Ana Silva (filha)"
                kind = FieldKind.Name
                onValue = { onForm(form.copy(emergencyContactName = it)) }
            }
        }
        formField(caption = "Telefone do contato") {
            TextField {
                label = "Telefone do contato"
                value = form.emergencyContactPhone
                placeholder = "(81) 99888-7766"
                kind = FieldKind.Phone
                onValue = { onForm(form.copy(emergencyContactPhone = it)) }
            }
        }
    }
    if (saved) {
        infoAlert("Configurações salvas com sucesso!") { checkIcon(it) }
    }
    primaryButton(if (busy) "Salvando…" else "Salvar alterações", enabled = !busy, onPress = onSave)
}

private fun ChildrenBuilder.securityTab(
    profile: AccountProfile?,
    busy: Boolean,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    passwordError: String?,
    passwordChanged: String?,
    confirmingDeletion: Boolean,
    onCurrent: (String) -> Unit,
    onNew: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSubmitPassword: () -> Unit,
    onLogout: () -> Unit,
    onAskDeletion: () -> Unit,
    onKeepAccount: () -> Unit,
    onConfirmDeletion: () -> Unit,
) {
    View {
        style = Styles.card
        sectionTitle("Alterar senha")
        formField(caption = "Senha atual", required = true) {
            TextField {
                label = "Senha atual"
                value = currentPassword
                placeholder = "••••••••"
                kind = FieldKind.Password
                onValue = onCurrent
            }
        }
        formField(caption = "Nova senha", required = true, hint = "Mínimo 6 caracteres") {
            TextField {
                label = "Nova senha"
                value = newPassword
                placeholder = "••••••••"
                kind = FieldKind.NewPassword
                onValue = onNew
            }
        }
        formField(caption = "Confirmar nova senha", required = true) {
            TextField {
                label = "Confirmar nova senha"
                value = confirmPassword
                placeholder = "••••••••"
                kind = FieldKind.NewPassword
                onValue = onConfirm
                onSubmit = onSubmitPassword
            }
        }
        passwordError?.let { errorAlert(it) }
        passwordChanged?.let { message -> infoAlert(message) { checkIcon(it) } }
        secondaryButton(if (busy) "Aguarde…" else "Alterar senha", onPress = onSubmitPassword)
    }
    View {
        style = Styles.cardTight
        sectionTitle("Sessão")
        Pressable {
            style = pressable(Styles.dangerRow)
            accessibilityRole = "button"
            this.onPress = { onLogout() }
            logoutIcon(Palette.ERROR_TEXT)
            Text {
                style = Styles.dangerText
                +"Encerrar sessão"
            }
        }
    }
    View {
        style = Styles.cardTight
        sectionTitle("Conta")
        val requestedAt = profile?.deletionRequestedAt
        if (requestedAt != null) {
            infoAlert(
                "Pedido de exclusão registrado em ${formatStamp(requestedAt)}. Você receberá orientações por e-mail.",
            ) { checkIcon(it) }
        } else if (confirmingDeletion) {
            Text {
                style = Styles.noteText
                accessibilityRole = "alert"
                +"Deseja solicitar a exclusão da sua conta? A sessão será encerrada em todos os aparelhos."
            }
            View {
                style = Styles.buttonRow
                secondaryButton("Manter conta", grow = true, onPress = onKeepAccount)
                primaryButton(
                    if (busy) "Enviando…" else "Sim, solicitar",
                    enabled = !busy,
                    grow = true,
                    onPress = onConfirmDeletion,
                )
            }
        } else {
            Pressable {
                style = pressable(Styles.dangerRow, if (profile == null) Styles.buttonDisabled else null)
                accessibilityRole = "button"
                disabled = profile == null
                ariaDisabled = profile == null
                this.onPress = { onAskDeletion() }
                trashIcon(Palette.ERROR_TEXT)
                Text {
                    style = Styles.dangerText
                    +"Solicitar exclusão da conta"
                }
            }
        }
        Text {
            style = Styles.noteText
            +"A exclusão é verificada conforme prazo legal de retenção de dados (LGPD). Você receberá orientações por e-mail."
        }
    }
}

private fun ChildrenBuilder.privacyTab(
    profile: AccountProfile,
    busy: Boolean,
    onNotifications: (Boolean) -> Unit,
    onPrivacyPolicy: () -> Unit,
) {
    View {
        style = Styles.card
        sectionTitle("Meus consentimentos")
        requiredConsent(
            "Política de Privacidade",
            "Li e estou ciente do tratamento de dados.",
            profile.acceptedPrivacyPolicy,
        )
        requiredConsent(
            "Tratamento para agendamento",
            "Uso dos dados para agendamento de consultas.",
            profile.acceptedTreatment,
        )
        CheckBox {
            checked = profile.wantsNotifications
            label = "Comunicações e lembretes"
            style = Styles.consentRow
            boxStyle = Styles.consentCheckbox
            onToggle = { if (!busy) onNotifications(it) }
            View {
                style = Styles.consentRowCopy
                Text {
                    style = Styles.consentRowTitle
                    +"Comunicações e lembretes"
                }
                Text {
                    style = Styles.consentRowText
                    +"Receber lembretes de consulta por WhatsApp/e-mail."
                }
            }
        }
    }
    Pressable {
        style = pressable(Styles.linkCard)
        accessibilityRole = "button"
        accessibilityLabel = "Política de Privacidade e Direitos. Ver como seus dados são tratados"
        onPress = { onPrivacyPolicy() }
        lockIcon(Palette.BLUE)
        View {
            style = Styles.quickCopy
            Text {
                style = Styles.quickTitle
                +"Política de Privacidade e Direitos"
            }
            Text {
                style = Styles.quickSubtitle
                +"Ver como seus dados são tratados"
            }
        }
        chevronIcon(Palette.INK)
    }
    View {
        style = Styles.cardCompact
        Text {
            style = Styles.consentRowTitle
            +"Exercer direitos (LGPD)"
        }
        Text {
            style = Styles.policyText
            +"Você pode solicitar acesso, correção, portabilidade ou eliminação dos seus dados. Entre em contato via e-mail."
        }
        textLink("$LGPD_EMAIL →") {
            Linking.openURL("mailto:$LGPD_EMAIL")
        }
    }
}

/** Mandatory consents are shown as given at registration. They cannot be withdrawn here. */
private fun ChildrenBuilder.requiredConsent(title: String, description: String, given: Boolean) {
    View {
        key = Key(title)
        style = Styles.consentRow
        accessible = true
        accessibilityLabel = "$title (obrigatório): ${if (given) "aceito" else "não aceito"}. $description"
        View {
            style = arrayOf(Styles.checkbox, Styles.consentCheckbox, if (given) Styles.checkboxChecked else null)
            if (given) {
                checkIcon(Palette.WHITE)
            }
        }
        View {
            style = Styles.consentRowCopy
            Text {
                style = Styles.consentRowTitle
                +title
                Text {
                    style = Styles.required
                    +" (obrigatório)"
                }
            }
            Text {
                style = Styles.consentRowText
                +description
            }
        }
    }
}

private fun ChildrenBuilder.summaryLine(label: String, value: String, last: Boolean = false) {
    View {
        key = Key(label)
        style = arrayOf(Styles.summaryRow, if (last) Styles.summaryRowLast else null)
        Text {
            style = Styles.summaryLabel
            +label
        }
        Text {
            style = Styles.summaryValue
            +value
        }
    }
}

private fun AccountProfile.toForm() = UpdateProfileRequest(
    fullName = fullName,
    socialName = socialName,
    email = email,
    primaryPhone = Phone.digits(primaryPhone),
    primaryPhoneWhatsapp = primaryPhoneWhatsapp,
    secondaryPhone = Phone.digits(secondaryPhone),
    secondaryPhoneWhatsapp = secondaryPhoneWhatsapp,
    emergencyContactName = emergencyContactName,
    emergencyContactPhone = Phone.digits(emergencyContactPhone),
)

/** `dd/MM/yyyy` in the device time zone, or a dash when the server never recorded the date. */
private fun formatStamp(millis: Long?): String {
    if (millis == null) {
        return "—"
    }
    val date = Date(millis.toDouble())
    val day = date.getDate().toString().padStart(2, '0')
    val month = (date.getMonth() + 1).toString().padStart(2, '0')
    return "$day/$month/${date.getFullYear()}"
}
