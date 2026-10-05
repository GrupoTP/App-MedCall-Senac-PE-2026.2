package grupotp.recifesaudavel.app

import react.FC
import react.Props
import react.create
import react.dom.client.createRoot
import react.dom.html.ReactHTML.h1
import web.dom.ElementId
import web.dom.document
import web.html.HTMLElement

fun main() {
    val container = document.getElementById(ElementId("root")) as HTMLElement
    createRoot(container).render(App.create())
}

private val App = FC<Props> {
    h1 {
        +"Recife Saudável"
    }
}
