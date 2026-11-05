package grupotp.medcall.app.ui.component.alert

import grupotp.medcall.app.domain.RemoteError
import grupotp.medcall.app.ui.component.button.textLink
import grupotp.medcall.app.ui.format.ErrorMessages
import react.ChildrenBuilder

/**
 * Shown when the server refused the saved token. Signing in again is the only way out.
 */
internal fun ChildrenBuilder.sessionExpiredAlert(
    error: RemoteError?,
    onSignOut: () -> Unit,
    message: (RemoteError) -> String = ErrorMessages::scheduling,
) {
    if (error == null) {
        return
    }
    errorAlert(message(error))
    if (error == RemoteError.SessionExpired) {
        textLink("Entrar de novo", onPress = onSignOut)
    }
}
