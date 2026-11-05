package grupotp.medcall.app.ui.component.alert

import grupotp.medcall.app.ui.component.text.emoji
import grupotp.medcall.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder

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
