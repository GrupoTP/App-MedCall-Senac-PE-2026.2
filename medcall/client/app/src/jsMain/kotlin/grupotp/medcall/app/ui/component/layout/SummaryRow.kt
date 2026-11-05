package grupotp.recifesaudavel.app.ui.component.layout

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder

internal fun ChildrenBuilder.summaryRow(label: String, value: String, last: Boolean = false, loose: Boolean = false) {
    View {
        style = arrayOf(
            Styles.summaryRow,
            if (loose) Styles.summaryRowLoose else null,
            if (last) Styles.summaryRowLast else null,
        )
        Text {
            style = Styles.summaryLabel
            +label
        }
        Text {
            style = Styles.summaryValue
            +value
        }
    }
}
