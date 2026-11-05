package grupotp.recifesaudavel.app.ui.component.text

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import react.ChildrenBuilder

internal fun ChildrenBuilder.strongText(value: String) {
    Text {
        style = Styles.infoAlertStrong
        +value
    }
}
