package grupotp.recifesaudavel.app.ui.component.alert

import grupotp.recifesaudavel.app.ui.icon.alertIcon
import grupotp.recifesaudavel.app.ui.theme.Palette
import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder

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
