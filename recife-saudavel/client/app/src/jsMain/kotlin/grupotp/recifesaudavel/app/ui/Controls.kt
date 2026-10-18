package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.common.phone.Phone
import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.Modal
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.TextInput
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props
import react.PropsWithChildren
import react.useState

internal external interface ScreenHeaderProps : Props {
    var title: String
    var onBack: () -> Unit
    var menuSpace: Boolean?
}

internal val ScreenHeader = FC<ScreenHeaderProps> { props ->
    val insets = useSafeAreaInsets()
    View {
        style = arrayOf(Styles.screenHeader, json { paddingTop = insets.top })
        View {
            style = Styles.screenHeaderInner
            if (props.menuSpace == true) {
                // The floating menu button sits over the left edge of every signed-in header.
                View { style = Styles.menuSpace }
            }
            Pressable {
                style = pressable(Styles.backButton)
                accessibilityRole = "button"
                accessibilityLabel = "Voltar"
                onPress = props.onBack
                backIcon(Palette.BLUE)
            }
            Text {
                style = Styles.screenTitle
                accessibilityRole = "header"
                +props.title
            }
        }
    }
}

internal val AppFooter = FC<Props> {
    val insets = useSafeAreaInsets()
    View {
        style = arrayOf(Styles.appFooter, json { paddingBottom = 12 + insets.bottom })
        Text {
            style = Styles.appFooterText
            +FOOTER_TEXT
        }
    }
}

internal fun ChildrenBuilder.errorAlert(message: String) {
    View {
        style = Styles.alert
        accessibilityRole = "alert"
        accessibilityLiveRegion = "polite"
        View {
            style = Styles.alertIcon
            alertIcon(Palette.ERROR_TEXT)
        }
        Text {
            style = Styles.alertText
            +message
        }
    }
}

/**
 * The prototype ClinicalDisclaimer: the app never diagnoses and points to SAMU.
 */
internal fun ChildrenBuilder.clinicalDisclaimer() {
    View {
        style = Styles.disclaimer
        View {
            style = Styles.alertIcon
            alertIcon(Palette.WARNING_TEXT)
        }
        Text {
            style = Styles.disclaimerText
            +"O Recife Saudável não realiza diagnóstico médico e não substitui atendimento profissional. Em emergência, ligue 192 (SAMU)."
        }
    }
}

internal fun ChildrenBuilder.sectionTitle(text: String) {
    View {
        style = Styles.sectionTitle
        View { style = Styles.sectionRule }
        Text {
            style = Styles.sectionTitleText
            accessibilityRole = "header"
            +text
        }
        View { style = Styles.sectionRule }
    }
}

internal fun ChildrenBuilder.formField(
    caption: String,
    required: Boolean = false,
    hint: String? = null,
    content: ChildrenBuilder.() -> Unit,
) {
    View {
        style = Styles.field
        Text {
            style = Styles.fieldLabel
            +caption
            if (required) {
                Text {
                    style = Styles.required
                    accessibilityLabel = "campo obrigatório"
                    +" *"
                }
            }
        }
        content()
        if (hint != null) {
            Text {
                style = Styles.fieldHint
                +hint
            }
        }
    }
}

internal enum class FieldKind {
    Text,
    Name,
    Email,
    Password,
    NewPassword,
    Phone,
    Date,
}

internal external interface TextFieldProps : Props {
    var label: String
    var value: String
    var placeholder: String
    var kind: FieldKind
    var obscured: Boolean?
    var withIcon: Boolean?
    var multiline: Boolean?
    var onValue: (String) -> Unit
    var onSubmit: (() -> Unit)?
}

/**
 * Text input with the prototype focus ring. React Native has no :focus selector, so the state is local.
 */
internal val TextField = FC<TextFieldProps> { props ->
    var focused by useState(false)
    val kind = props.kind
    TextInput {
        style = arrayOf(
            Styles.fieldInput,
            if (props.withIcon == true) Styles.fieldInputWithIcon else null,
            if (props.multiline == true) Styles.fieldInputMultiline else null,
            if (focused) Styles.fieldInputFocused else null,
        )
        if (props.multiline == true) {
            multiline = true
            numberOfLines = 3
        }
        accessibilityLabel = props.label
        val isPhone = kind == FieldKind.Phone
        val shown = if (isPhone) Phone.format(props.value) else props.value
        value = shown
        placeholder = props.placeholder
        placeholderTextColor = Palette.MUTED
        cursorColor = Palette.BLUE
        selectionColor = Palette.BLUE
        underlineColorAndroid = "transparent"
        secureTextEntry = props.obscured ?: (kind == FieldKind.Password || kind == FieldKind.NewPassword)
        autoCorrect = kind == FieldKind.Text
        autoCapitalize = when (kind) {
            FieldKind.Text -> "sentences"
            FieldKind.Name -> "words"
            FieldKind.Email, FieldKind.Password, FieldKind.NewPassword, FieldKind.Phone, FieldKind.Date -> "none"
        }
        keyboardType = when (kind) {
            FieldKind.Email -> "email-address"
            FieldKind.Phone -> "phone-pad"
            FieldKind.Date -> "number-pad"
            else -> "default"
        }
        autoComplete = when (kind) {
            FieldKind.Text -> "off"
            FieldKind.Name -> "name"
            FieldKind.Email -> "email"
            FieldKind.Password -> "current-password"
            FieldKind.NewPassword -> "new-password"
            FieldKind.Phone -> "tel"
            FieldKind.Date -> "birthdate-full"
        }
        val submit = props.onSubmit
        if (submit != null) {
            returnKeyType = "go"
            onSubmitEditing = submit
        }
        onFocus = { focused = true }
        onBlur = { focused = false }
        onChangeText = if (isPhone) {
            { text ->
                // The state holds digits only. Erasing a mask character keeps the digits, so drop the last one.
                val typed = Phone.limited(text)
                val erasedMask = text.length < shown.length && typed == Phone.digitsOf(props.value)
                props.onValue(if (erasedMask) typed.dropLast(1) else typed)
            }
        } else {
            props.onValue
        }
    }
}

internal external interface CheckBoxProps : PropsWithChildren {
    var checked: Boolean
    var label: String
    var onToggle: (Boolean) -> Unit
    var style: dynamic
    var boxStyle: dynamic
}

/**
 * Pressable row with a square check mark. Neither platform ships a native checkbox for React Native.
 */
internal val CheckBox = FC<CheckBoxProps> { props ->
    Pressable {
        style = props.style
        accessibilityRole = "checkbox"
        accessibilityLabel = props.label
        ariaChecked = props.checked
        onPress = { props.onToggle(!props.checked) }
        View {
            style = arrayOf(Styles.checkbox, props.boxStyle, if (props.checked) Styles.checkboxChecked else null)
            if (props.checked) {
                checkIcon(Palette.WHITE)
            }
        }
        +props.children
    }
}

internal fun ChildrenBuilder.phoneField(
    caption: String,
    value: Phone,
    whatsapp: Boolean,
    required: Boolean = false,
    onValue: (Phone) -> Unit,
    onWhatsapp: (Boolean) -> Unit,
) {
    formField(caption = caption, required = required) {
        TextField {
            label = caption
            this.value = value.digits
            placeholder = "(81) 99999-9999"
            kind = FieldKind.Phone
            this.onValue = { onValue(Phone(it)) }
        }
        CheckBox {
            checked = whatsapp
            label = "$caption: WhatsApp disponível"
            style = Styles.checkLine
            onToggle = onWhatsapp
            Text {
                style = Styles.checkCopy
                Text {
                    style = Styles.whatsapp
                    +"WhatsApp"
                }
                +" disponível"
            }
        }
    }
}

internal fun ChildrenBuilder.primaryButton(
    text: String,
    enabled: Boolean = true,
    grow: Boolean = false,
    onPress: () -> Unit,
) {
    Pressable {
        style = pressable(
            Styles.primaryButton,
            if (grow) Styles.buttonGrow else null,
            if (enabled) null else Styles.buttonDisabled,
        )
        accessibilityRole = "button"
        disabled = !enabled
        ariaDisabled = !enabled
        this.onPress = onPress
        Text {
            style = Styles.primaryButtonText
            +text
        }
    }
}

internal fun ChildrenBuilder.secondaryButton(text: String, grow: Boolean = false, onPress: () -> Unit) {
    Pressable {
        style = pressable(Styles.secondaryButton, if (grow) Styles.buttonGrow else null)
        accessibilityRole = "button"
        this.onPress = onPress
        Text {
            style = Styles.secondaryButtonText
            +text
        }
    }
}

/**
 * The prototype draws its notices with emoji, so the icons here are emoji too.
 */
internal fun ChildrenBuilder.emoji(symbol: String, style: dynamic = Styles.emoji) {
    Text {
        this.style = style
        accessible = false
        +symbol
    }
}

internal fun ChildrenBuilder.infoAlert(message: String, icon: ChildrenBuilder.(color: String) -> Unit = { emoji("ℹ️") }) {
    infoAlertRich(icon) { +message }
}

/**
 * Info notice whose text can hold bold parts, such as a phone number.
 */
internal fun ChildrenBuilder.infoAlertRich(
    icon: ChildrenBuilder.(color: String) -> Unit = { emoji("ℹ️") },
    content: ChildrenBuilder.() -> Unit,
) {
    View {
        style = Styles.infoAlert
        View {
            style = Styles.alertIcon
            icon(this, Palette.GREEN_TEXT)
        }
        Text {
            style = Styles.infoAlertText
            content()
        }
    }
}

internal fun ChildrenBuilder.strongText(value: String) {
    Text {
        style = Styles.infoAlertStrong
        +value
    }
}

internal fun ChildrenBuilder.warnAlert(message: String, icon: String = "⚠️") {
    View {
        style = Styles.warnAlert
        View {
            style = Styles.alertIcon
            emoji(icon)
        }
        Text {
            style = Styles.warnAlertText
            +message
        }
    }
}

/**
 * Selectable option. React Native has no `<select>`, so lists of choices are rows of chips.
 */
internal fun ChildrenBuilder.choiceChip(
    text: String,
    selected: Boolean,
    style: dynamic = Styles.chip,
    accessibilityLabel: String = text,
    onPress: () -> Unit,
) {
    Pressable {
        this.style = pressable(style, if (selected) Styles.chipSelected else null)
        accessibilityRole = "radio"
        this.accessibilityLabel = accessibilityLabel
        ariaChecked = selected
        this.onPress = onPress
        Text {
            this.style = if (selected) Styles.chipTextSelected else Styles.chipText
            +text
        }
    }
}

internal fun ChildrenBuilder.fieldError(message: String?) {
    if (message != null) {
        Text {
            style = Styles.fieldError
            accessibilityRole = "alert"
            +message
        }
    }
}

internal external interface StepIndicatorProps : Props {
    var current: Int
    var labels: Array<String>
}

/**
 * Port of the prototype step indicator: counter, remaining steps, current label and segmented bars.
 * The whole state is also announced as one sentence for screen readers.
 */
internal val StepIndicator = FC<StepIndicatorProps> { props ->
    val total = props.labels.size
    val current = props.current
    val remaining = total - current - 1
    val last = remaining == 0
    val label = props.labels[current]
    val done = if (current == 1) "1 etapa concluída" else "$current etapas concluídas"
    val left = if (remaining == 1) "Falta 1 etapa" else "Faltam $remaining etapas"
    View {
        style = Styles.steps
        View {
            style = Styles.stepsInner
            accessible = true
            accessibilityRole = "progressbar"
            accessibilityLabel = if (last) {
                "Etapa ${current + 1} de $total: $label. Última etapa. $done."
            } else {
                "Etapa ${current + 1} de $total: $label. $done. $left."
            }
            View {
                style = Styles.stepsTop
                Text {
                    style = Styles.stepsCounter
                    +"Etapa ${current + 1} de $total"
                }
                View {
                    style = arrayOf(Styles.stepsRemaining, if (last) Styles.stepsRemainingLast else null)
                    Text {
                        style = if (last) Styles.stepsRemainingTextLast else Styles.stepsRemainingText
                        +(if (last) "✓ Última etapa" else left)
                    }
                }
            }
            Text {
                style = Styles.stepsLabel
                +label
            }
            View {
                style = Styles.stepsBars
                for (index in 0 until total) {
                    View {
                        key = Key(index.toString())
                        style = Styles.stepsColumn
                        Text {
                            style = arrayOf(
                                Styles.stepsNumber,
                                when {
                                    index < current -> Styles.stepsNumberDone
                                    index == current -> null
                                    else -> Styles.stepsNumberPending
                                },
                            )
                            +(index + 1).toString()
                        }
                        View {
                            style = arrayOf(
                                Styles.stepsBar,
                                when {
                                    index < current -> Styles.stepsBarDone
                                    index == current -> Styles.stepsBarActive
                                    else -> null
                                },
                            )
                        }
                    }
                }
            }
            Text {
                style = Styles.stepsNote
                +(if (current == 0) "Nenhuma etapa concluída ainda" else done)
                if (last) {
                    +" · Complete esta para finalizar"
                }
            }
        }
    }
}

internal fun ChildrenBuilder.textLink(text: String, centered: Boolean = false, onPress: () -> Unit) {
    Pressable {
        style = if (centered) Styles.textLinkCenter else Styles.textLink
        accessibilityRole = "link"
        this.onPress = onPress
        Text {
            style = if (centered) Styles.textLinkTextCenter else Styles.textLinkText
            +text
        }
    }
}

/**
 * Pressable style that shrinks slightly while pressed, like the prototype :active rule.
 */
internal fun pressable(vararg styles: dynamic): (dynamic) -> dynamic = { state ->
    if (state.pressed == true) arrayOf(*styles, Styles.pressed) else arrayOf(*styles)
}

/**
 * Question in a modal card with two answers. [onDismiss] runs on the Android back button and on
 * a tap outside the card, and should leave things as they were.
 */
internal fun ChildrenBuilder.confirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String,
    declineText: String,
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit,
) {
    Modal {
        this.visible = visible
        transparent = true
        animationType = "fade"
        onRequestClose = onDismiss
        View {
            style = Styles.dialogBackdrop
            Pressable {
                style = Styles.fill
                accessibilityLabel = "Fechar"
                this.onPress = onDismiss
            }
            View {
                style = Styles.dialogCard
                role = "alertdialog"
                ariaModal = true
                ariaLabel = title
                Text {
                    style = Styles.dialogTitle
                    accessibilityRole = "header"
                    +title
                }
                Text {
                    style = Styles.dialogText
                    +message
                }
                View {
                    style = Styles.buttonRow
                    secondaryButton(declineText, grow = true, onPress = onDecline)
                    primaryButton(confirmText, grow = true, onPress = onConfirm)
                }
            }
        }
    }
}
