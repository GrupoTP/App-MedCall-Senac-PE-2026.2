package grupotp.recifesaudavel.app.ui.component.alert

import grupotp.recifesaudavel.app.ui.component.button.primaryButton
import grupotp.recifesaudavel.app.ui.component.button.secondaryButton
import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Modal
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder

/**
 * Question in a modal card with two answers. [onDismiss] runs on the Android back button and on
 * a tap outside the card, and should leave things as they were.
 */
internal fun ChildrenBuilder.confirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String,
    declineText: String,
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit,
) {
    Modal {
        this.visible = visible
        transparent = true
        animationType = "fade"
        onRequestClose = onDismiss
        View {
            style = Styles.dialogBackdrop
            Pressable {
                style = Styles.fill
                accessibilityLabel = "Fechar"
                this.onPress = onDismiss
            }
            View {
                style = Styles.dialogCard
                role = "alertdialog"
                ariaModal = true
                ariaLabel = title
                Text {
                    style = Styles.dialogTitle
                    accessibilityRole = "header"
                    +title
                }
                Text {
                    style = Styles.dialogText
                    +message
                }
                View {
                    style = Styles.buttonRow
                    secondaryButton(declineText, grow = true, onPress = onDecline)
                    primaryButton(confirmText, grow = true, onPress = onConfirm)
                }
            }
        }
    }
}
