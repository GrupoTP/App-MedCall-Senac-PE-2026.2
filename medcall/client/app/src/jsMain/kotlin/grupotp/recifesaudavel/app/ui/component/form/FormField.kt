package grupotp.recifesaudavel.app.ui.component.form

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder

internal fun ChildrenBuilder.formField(
    caption: String,
    required: Boolean = false,
    hint: String? = null,
    content: ChildrenBuilder.() -> Unit,
) {
    View {
        style = Styles.field
        Text {
            style = Styles.fieldLabel
            +caption
            if (required) {
                Text {
                    style = Styles.required
                    accessibilityLabel = "campo obrigatório"
                    +" *"
                }
            }
        }
        content()
        if (hint != null) {
            Text {
                style = Styles.fieldHint
                +hint
            }
        }
    }
}
