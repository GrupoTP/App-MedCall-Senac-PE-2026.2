package grupotp.medcall.admin.ui

import web.html.ButtonType
import web.html.InputType

/**
 * Values of `type` attributes. The wrappers declare them as extensions named like the HTML
 * builders (`button`, `date`...), which clash with ReactHTML in the same file.
 */
object Kinds {
    val button: ButtonType = "button".unsafeCast<ButtonType>()
    val submit: ButtonType = "submit".unsafeCast<ButtonType>()
    val text: InputType = "text".unsafeCast<InputType>()
    val email: InputType = "email".unsafeCast<InputType>()
    val password: InputType = "password".unsafeCast<InputType>()
    val date: InputType = "date".unsafeCast<InputType>()
    val url: InputType = "url".unsafeCast<InputType>()
    val checkbox: InputType = "checkbox".unsafeCast<InputType>()
    val search: InputType = "search".unsafeCast<InputType>()
}
