package grupotp.recifesaudavel.app.ui.component.layout

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder

internal fun ChildrenBuilder.sectionTitle(text: String) {
    View {
        style = Styles.sectionTitle
        View { style = Styles.sectionRule }
        Text {
            style = Styles.sectionTitleText
            accessibilityRole = "header"
            +text
        }
        View { style = Styles.sectionRule }
    }
}
