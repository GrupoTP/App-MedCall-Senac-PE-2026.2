package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.Session
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.admin.ui.secretOnce
import react.ChildrenBuilder
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import web.cssom.ClassName
import web.navigator.navigator

/** Shows a secret once, with a copy button. */
internal fun ChildrenBuilder.secretModal(session: Session, shown: Pair<String, String>?, onClose: () -> Unit) {
    if (shown == null) return
    modal("Guarde este valor agora", onClose, footer = {
        button { type = Kinds.button; className = ClassName("btn btn-primary"); onClick = { onClose() }; +"Já guardei" }
    }) {
        secretOnce(shown.first, shown.second) {
            navigator.clipboard.writeTextAsync(shown.second)
            session.notify("Copiado.", false)
        }
    }
}

