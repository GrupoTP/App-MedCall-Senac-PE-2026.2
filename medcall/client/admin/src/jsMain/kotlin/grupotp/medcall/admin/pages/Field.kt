package grupotp.medcall.admin.pages

import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import web.cssom.ClassName
import web.dom.ElementId
import web.html.InputType

/** A labeled Bootstrap input. */
fun ChildrenBuilder.field(
    id: String,
    label: String,
    type: InputType,
    value: String,
    autoComplete: String? = null,
    help: String? = null,
    required: Boolean = true,
    onChange: (String) -> Unit,
) {
    div {
        className = ClassName("mb-3")
        label { className = ClassName("form-label"); htmlFor = ElementId(id); +label }
        input {
            this.id = ElementId(id)
            this.type = type
            className = ClassName("form-control")
            this.value = value
            this.required = required
            if (autoComplete != null) asDynamic().autoComplete = autoComplete
            this.onChange = { event -> onChange(event.target.value) }
        }
        if (help != null) div { className = ClassName("form-text"); +help }
    }
}

