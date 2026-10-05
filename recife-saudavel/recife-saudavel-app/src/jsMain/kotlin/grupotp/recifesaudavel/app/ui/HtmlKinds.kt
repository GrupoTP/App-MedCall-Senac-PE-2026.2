package grupotp.recifesaudavel.app.ui

import js.reflect.unsafeCast
import web.html.ButtonType
import web.html.InputType

internal val buttonKind: ButtonType = unsafeCast("button")
internal val submitKind: ButtonType = unsafeCast("submit")
internal val checkboxKind: InputType = unsafeCast("checkbox")
internal val emailKind: InputType = unsafeCast("email")
internal val passwordKind: InputType = unsafeCast("password")
internal val telKind: InputType = unsafeCast("tel")
internal val textKind: InputType = unsafeCast("text")
