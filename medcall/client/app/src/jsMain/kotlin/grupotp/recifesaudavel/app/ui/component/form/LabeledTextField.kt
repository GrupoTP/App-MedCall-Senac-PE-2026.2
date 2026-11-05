package grupotp.recifesaudavel.app.ui.component.form

import react.ChildrenBuilder

/**
 * A [TextField] under its caption, with the required mark and the hint of [formField].
 */
internal fun ChildrenBuilder.labeledTextField(
    caption: String,
    value: String,
    placeholder: String,
    kind: FieldKind = FieldKind.Text,
    multiline: Boolean = false,
    required: Boolean = false,
    hint: String? = null,
    onValue: (String) -> Unit,
) {
    formField(caption = caption, required = required, hint = hint) {
        TextField {
            label = caption
            this.value = value
            this.placeholder = placeholder
            this.kind = kind
            this.multiline = multiline
            this.onValue = onValue
        }
    }
}
