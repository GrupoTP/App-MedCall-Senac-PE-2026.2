package grupotp.medcall.app.ui.component.form

import grupotp.medcall.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import react.ChildrenBuilder

internal fun ChildrenBuilder.fieldError(message: String?) {
    if (message != null) {
        Text {
            style = Styles.fieldError
            accessibilityRole = "alert"
            +message
        }
    }
}
