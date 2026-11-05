package grupotp.recifesaudavel.app.ui.component.layout

import grupotp.recifesaudavel.app.ui.component.button.pressable
import grupotp.recifesaudavel.app.ui.icon.backIcon
import grupotp.recifesaudavel.app.ui.theme.Palette
import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.FC
import react.Props

internal external interface ScreenHeaderProps : Props {
    var title: String
    var onBack: () -> Unit
    var menuSpace: Boolean?
}

internal val ScreenHeader = FC<ScreenHeaderProps> { props ->
    val insets = useSafeAreaInsets()
    View {
        style = arrayOf(Styles.screenHeader, json { paddingTop = insets.top })
        View {
            style = Styles.screenHeaderInner
            if (props.menuSpace == true) {
                // The floating menu button sits over the left edge of every signed-in header.
                View { style = Styles.menuSpace }
            }
            Pressable {
                style = pressable(Styles.backButton)
                accessibilityRole = "button"
                accessibilityLabel = "Voltar"
                onPress = props.onBack
                backIcon(Palette.BLUE)
            }
            Text {
                style = Styles.screenTitle
                accessibilityRole = "header"
                +props.title
            }
        }
    }
}
