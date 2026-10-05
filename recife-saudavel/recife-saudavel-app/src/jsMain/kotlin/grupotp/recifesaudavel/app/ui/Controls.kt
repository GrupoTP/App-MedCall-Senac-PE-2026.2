package grupotp.recifesaudavel.app.ui

import react.ChildrenBuilder
import react.dom.aria.AriaRole
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import web.cssom.ClassName
import web.dom.ElementId
import web.html.InputType

internal fun ChildrenBuilder.screenHeader(title: String, onBack: () -> Unit) {
    div {
        className = classNames("screen-header")
        div {
            className = classNames("screen-header-inner")
            button {
                type = buttonKind
                className = classNames("back-button")
                ariaLabel = "Voltar"
                onClick = { onBack() }
                backIcon()
            }
            h1 {
                className = classNames("screen-title")
                +title
            }
        }
    }
}

internal fun ChildrenBuilder.appFooter() {
    div {
        className = classNames("app-footer")
        p {
            +FOOTER_TEXT
        }
    }
}

internal fun ChildrenBuilder.errorAlert(message: String) {
    div {
        className = classNames("alert alert-error")
        role = AriaRole.alert
        alertIcon()
        span {
            +message
        }
    }
}

internal fun ChildrenBuilder.sectionTitle(text: String) {
    div {
        className = classNames("section-title")
        span {
            +text
        }
    }
}

internal fun ChildrenBuilder.formField(
    caption: String,
    required: Boolean = false,
    hint: String? = null,
    content: ChildrenBuilder.() -> Unit,
) {
    div {
        className = classNames("field")
        label {
            className = classNames("field-label")
            +caption
            if (required) {
                span {
                    className = classNames("required")
                    ariaLabel = "campo obrigatório"
                    +"*"
                }
            }
        }
        content()
        if (hint != null) {
            p {
                className = classNames("field-hint")
                +hint
            }
        }
    }
}

internal fun ChildrenBuilder.textInput(
    id: String,
    value: String,
    placeholder: String,
    type: InputType = textKind,
    extraClass: String? = null,
    onValue: (String) -> Unit,
) {
    input {
        this.id = ElementId(id)
        this.type = type
        this.value = value
        this.placeholder = placeholder
        className = classNames("field-input", extraClass)
        onChange = { event ->
            onValue(event.target.value)
        }
    }
}

internal fun ChildrenBuilder.phoneField(
    caption: String,
    value: String,
    whatsapp: Boolean,
    required: Boolean = false,
    onValue: (String) -> Unit,
    onWhatsapp: (Boolean) -> Unit,
) {
    formField(caption = caption, required = required) {
        textInput(
            id = caption,
            value = value,
            placeholder = "(81) 99999-9999",
            type = telKind,
            onValue = onValue,
        )
        label {
            className = classNames("check-line")
            input {
                type = checkboxKind
                checked = whatsapp
                onChange = { event ->
                    onWhatsapp(event.target.checked)
                }
            }
            span {
                className = classNames("check-copy")
                span {
                    className = classNames("whatsapp")
                    +"WhatsApp"
                }
                +" disponível"
            }
        }
    }
}

internal fun classNames(vararg names: String?): ClassName =
    ClassName(names.filterNotNull().joinToString(" "))
