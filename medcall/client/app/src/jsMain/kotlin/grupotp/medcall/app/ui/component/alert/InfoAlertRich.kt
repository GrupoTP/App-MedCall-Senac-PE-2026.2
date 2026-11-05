package grupotp.medcall.app.ui.component.alert

import grupotp.medcall.app.ui.component.text.emoji
import grupotp.medcall.app.ui.theme.Palette
import grupotp.medcall.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder

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
