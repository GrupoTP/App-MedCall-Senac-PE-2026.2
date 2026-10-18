package grupotp.recifesaudavel.app.ui.component.alert

import grupotp.recifesaudavel.app.domain.scheduling.SchedulingError
import grupotp.recifesaudavel.app.ui.component.button.textLink
import grupotp.recifesaudavel.app.ui.format.ErrorMessages
import react.ChildrenBuilder

/**
 * Shown when the server refused the saved token. Signing in again is the only way out.
 */
internal fun ChildrenBuilder.sessionExpiredAlert(
    error: SchedulingError?,
    onSignOut: () -> Unit,
    message: (SchedulingError) -> String = ErrorMessages::scheduling,
) {
    if (error == null) {
        return
    }
    errorAlert(message(error))
    if (error == SchedulingError.SessionExpired) {
        textLink("Entrar de novo", onPress = onSignOut)
    }
}
