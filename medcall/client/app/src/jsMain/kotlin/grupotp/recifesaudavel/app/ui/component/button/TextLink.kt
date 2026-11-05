package grupotp.recifesaudavel.app.ui.component.button

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import react.ChildrenBuilder

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
