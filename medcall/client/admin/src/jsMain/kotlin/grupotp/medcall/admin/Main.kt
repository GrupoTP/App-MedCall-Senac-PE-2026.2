package grupotp.medcall.admin

import grupotp.medcall.admin.ui.installTheme
import react.create
import react.dom.client.createRoot
import web.dom.ElementId
import web.dom.document

fun main() {
    installTheme()
    val container = document.getElementById(ElementId("root")) ?: error("#root not found")
    createRoot(container).render(App.create())
}

