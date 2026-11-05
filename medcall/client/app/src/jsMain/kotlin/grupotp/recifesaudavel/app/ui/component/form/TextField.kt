package grupotp.recifesaudavel.app.ui.component.form

import grupotp.recifesaudavel.app.ui.theme.Palette
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.common.account.Phone
import ojaynico.kotlin.react.native.component.TextInput
import react.FC
import react.Props
import react.useState

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
