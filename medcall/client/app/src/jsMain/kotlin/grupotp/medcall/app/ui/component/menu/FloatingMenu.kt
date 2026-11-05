package grupotp.recifesaudavel.app.ui.component.menu

import grupotp.recifesaudavel.app.ui.component.button.pressable
import grupotp.recifesaudavel.app.ui.icon.clipboardIcon
import grupotp.recifesaudavel.app.ui.icon.closeIcon
import grupotp.recifesaudavel.app.ui.icon.homeIcon
import grupotp.recifesaudavel.app.ui.icon.listIcon
import grupotp.recifesaudavel.app.ui.icon.lockIcon
import grupotp.recifesaudavel.app.ui.icon.logoutIcon
import grupotp.recifesaudavel.app.ui.icon.menuIcon
import grupotp.recifesaudavel.app.ui.icon.settingsIcon
import grupotp.recifesaudavel.app.ui.icon.smallCalendarIcon
import grupotp.recifesaudavel.app.ui.theme.Palette
import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.ChildrenBuilder
import react.FC
import react.Props
import react.useState

internal external interface FloatingMenuProps : Props {
    var displayName: String
    var active: MenuDestination?
    var onNavigate: (MenuDestination) -> Unit
    var onLogout: () -> Unit
}

private const val BUTTON_TOP = 16
private const val PANEL_OFFSET = 48

/**
 * Hamburger button fixed at the top-left of every signed-in screen, with the navigation and
 * account links of the prototype menu.
 */
internal val FloatingMenu = FC<FloatingMenuProps> { props ->
    var open by useState(false)
    val insets = useSafeAreaInsets()
    val top = BUTTON_TOP + insets.top

    fun go(destination: MenuDestination) {
        open = false
        props.onNavigate(destination)
    }

    View {
        style = arrayOf(Styles.menuLayer, json { pointerEvents = "box-none" })
        if (open) {
            Pressable {
                // Blurs the screen behind the menu on the web; native platforms ignore it and keep the dim layer.
                style = arrayOf(Styles.menuBackdrop, json { backdropFilter = "blur(2px)" })
                accessibilityRole = "button"
                accessibilityLabel = "Fechar menu"
                onPress = { open = false }
            }
            View {
                style = arrayOf(Styles.menuPanel, json { this.top = top + PANEL_OFFSET })
                View {
                    style = Styles.menuGreeting
                    Text {
                        style = Styles.menuGreetingHello
                        +"Olá,"
                    }
                    Text {
                        style = Styles.menuGreetingName
                        numberOfLines = 1
                        +props.displayName
                    }
                }
                View {
                    style = Styles.menuBody
                    menuLabel("Navegação")
                    menuItem("Início", MenuDestination.Home, props.active, { homeIcon(it) }, ::go)
                    menuItem("Pré-triagem", MenuDestination.PreTriage, props.active, { clipboardIcon(it) }, ::go)
                    menuItem("Agendar consulta", MenuDestination.Schedule, props.active, { smallCalendarIcon(it) }, ::go)
                    menuItem("Minhas consultas", MenuDestination.Appointments, props.active, { listIcon(it) }, ::go)
                    View { style = Styles.menuDivider }
                    menuLabel("Conta")
                    menuItem("Configurações", MenuDestination.Settings, props.active, { settingsIcon(it) }, ::go)
                    menuItem("Privacidade", MenuDestination.Privacy, props.active, { lockIcon(it) }, ::go)
                    View { style = Styles.menuDivider }
                    Pressable {
                        style = { state: dynamic ->
                            arrayOf(
                                Styles.menuItem,
                                if (state.hovered == true) Styles.menuItemDangerHover else json {},
                                if (state.pressed == true) Styles.pressed else json {},
                            )
                        }
                        accessibilityRole = "button"
                        onPress = {
                            open = false
                            props.onLogout()
                        }
                        logoutIcon(Palette.REQUIRED)
                        Text {
                            style = Styles.menuItemTextDanger
                            +"Sair"
                        }
                    }
                }
            }
        }
        Pressable {
            style = pressable(
                Styles.menuButton,
                json { this.top = top },
                if (open) Styles.menuButtonOpen else json {},
            )
            accessibilityRole = "button"
            accessibilityLabel = "Menu"
            accessibilityState = json { expanded = open }
            onPress = { open = !open }
            if (open) closeIcon(Palette.WHITE) else menuIcon(Palette.BLUE)
        }
    }
}

private fun ChildrenBuilder.menuLabel(label: String) {
    View {
        style = Styles.menuLabelBox
        Text {
            style = Styles.menuLabel
            +label
        }
    }
}

private fun ChildrenBuilder.menuItem(
    label: String,
    destination: MenuDestination,
    active: MenuDestination?,
    icon: ChildrenBuilder.(String) -> Unit,
    onPress: (MenuDestination) -> Unit,
) {
    val selected = destination == active
    Pressable {
        style = { state: dynamic ->
            val highlighted = selected || state.hovered == true
            arrayOf(
                Styles.menuItem,
                if (highlighted) Styles.menuItemActive else json {},
                if (state.pressed == true) Styles.pressed else json {},
            )
        }
        accessibilityRole = "button"
        accessibilityState = json { this.selected = selected }
        this.onPress = { onPress(destination) }
        icon(if (selected) Palette.BLUE else Palette.MUTED)
        Text {
            style = if (selected) Styles.menuItemTextActive else Styles.menuItemText
            +label
        }
        if (selected) {
            View { style = Styles.menuActiveDot }
        }
    }
}
