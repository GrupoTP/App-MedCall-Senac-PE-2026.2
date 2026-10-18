package grupotp.recifesaudavel.app.ui.component.alert

import grupotp.recifesaudavel.app.ui.component.text.emoji
import react.ChildrenBuilder

internal fun ChildrenBuilder.infoAlert(message: String, icon: ChildrenBuilder.(color: String) -> Unit = { emoji("ℹ️") }) {
    infoAlertRich(icon) { +message }
}
