package grupotp.recifesaudavel.app.ui.component.form

import grupotp.recifesaudavel.app.ui.icon.checkIcon
import grupotp.recifesaudavel.app.ui.theme.Palette
import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.PropsWithChildren

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
