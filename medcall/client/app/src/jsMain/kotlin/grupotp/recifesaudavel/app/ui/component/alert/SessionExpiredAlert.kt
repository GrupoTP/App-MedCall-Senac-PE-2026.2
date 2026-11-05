package grupotp.recifesaudavel.app.ui.component.alert

import grupotp.recifesaudavel.app.domain.RemoteError
import grupotp.recifesaudavel.app.ui.component.button.textLink
import grupotp.recifesaudavel.app.ui.format.ErrorMessages
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
