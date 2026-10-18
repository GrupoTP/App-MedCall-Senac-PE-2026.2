package grupotp.recifesaudavel.app.ui.component.button

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import react.ChildrenBuilder

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
