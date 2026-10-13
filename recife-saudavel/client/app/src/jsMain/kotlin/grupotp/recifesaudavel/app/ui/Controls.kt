package grupotp.recifesaudavel.app.ui

import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.TextInput
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.ChildrenBuilder
import react.FC
import react.Props
import react.PropsWithChildren
import react.useState

internal external interface ScreenHeaderProps : Props {
    var title: String
    var onBack: () -> Unit
}

internal val ScreenHeader = FC<ScreenHeaderProps> { props ->
    val insets = useSafeAreaInsets()
    View {
        style = arrayOf(Styles.screenHeader, json { paddingTop = insets.top })
        View {
            style = Styles.screenHeaderInner
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
}

internal external interface TextFieldProps : Props {
    var label: String
    var value: String
    var placeholder: String
    var kind: FieldKind
    var obscured: Boolean?
    var withIcon: Boolean?
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
            if (focused) Styles.fieldInputFocused else null,
        )
        accessibilityLabel = props.label
        value = props.value
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
            FieldKind.Email, FieldKind.Password, FieldKind.NewPassword, FieldKind.Phone -> "none"
        }
        keyboardType = when (kind) {
            FieldKind.Email -> "email-address"
            FieldKind.Phone -> "phone-pad"
            else -> "default"
        }
        autoComplete = when (kind) {
            FieldKind.Text -> "off"
            FieldKind.Name -> "name"
            FieldKind.Email -> "email"
            FieldKind.Password -> "current-password"
            FieldKind.NewPassword -> "new-password"
            FieldKind.Phone -> "tel"
        }
        val submit = props.onSubmit
        if (submit != null) {
            returnKeyType = "go"
            onSubmitEditing = submit
        }
        onFocus = { focused = true }
        onBlur = { focused = false }
        onChangeText = props.onValue
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
    value: String,
    whatsapp: Boolean,
    required: Boolean = false,
    onValue: (String) -> Unit,
    onWhatsapp: (Boolean) -> Unit,
) {
    formField(caption = caption, required = required) {
        TextField {
            label = caption
            this.value = value
            placeholder = "(81) 99999-9999"
            kind = FieldKind.Phone
            this.onValue = onValue
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

internal fun ChildrenBuilder.primaryButton(text: String, onPress: () -> Unit) {
    Pressable {
        style = pressable(Styles.primaryButton)
        accessibilityRole = "button"
        this.onPress = onPress
        Text {
            style = Styles.primaryButtonText
            +text
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
