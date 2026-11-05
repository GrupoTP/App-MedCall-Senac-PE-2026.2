package grupotp.recifesaudavel.app.ui.component.button

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import react.ChildrenBuilder

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
