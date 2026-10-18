package grupotp.recifesaudavel.app.ui.component.form

import grupotp.recifesaudavel.app.ui.component.button.pressable
import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import react.ChildrenBuilder

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
