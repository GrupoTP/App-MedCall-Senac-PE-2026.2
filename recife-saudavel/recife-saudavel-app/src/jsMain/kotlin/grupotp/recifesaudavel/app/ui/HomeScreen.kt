package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.ListUpcomingAppointments
import grupotp.recifesaudavel.app.usecase.NOT_IMPLEMENTED_MESSAGE
import grupotp.recifesaudavel.app.usecase.UpcomingAppointmentsResult
import grupotp.recifesaudavel.common.AuthenticatedAccount
import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.ChildrenBuilder
import react.FC
import react.Props
import react.useState
import kotlin.js.Date

internal external interface HomeScreenProps : Props {
    var account: AuthenticatedAccount
    var listUpcomingAppointments: ListUpcomingAppointments
}

internal val HomeScreen = FC<HomeScreenProps> { props ->
    var unavailableAction by useState<String?>(null)
    val insets = useSafeAreaInsets()
    val upcoming = when (props.listUpcomingAppointments()) {
        UpcomingAppointmentsResult.NotImplemented -> NOT_IMPLEMENTED_MESSAGE
    }
    val displayName = props.account.socialName.ifBlank { props.account.fullName }

    fun markUnavailable() {
        unavailableAction = NOT_IMPLEMENTED_MESSAGE
    }

    View {
        style = Styles.screen
        View {
            style = arrayOf(Styles.homeHeader, json { paddingTop = 16 + insets.top })
            verticalGradient(id = "home-gradient", from = Palette.BLUE, to = Palette.BLUE_HOVER)
            View {
                style = Styles.homeHeaderInner
                View {
                    style = Styles.homeHeaderTools
                    Pressable {
                        style = pressable(Styles.homeIconButton)
                        accessibilityRole = "button"
                        accessibilityLabel = "Configurações"
                        onPress = { markUnavailable() }
                        settingsIcon(Palette.WHITE)
                    }
                }
                Text {
                    style = Styles.homeGreeting
                    +"${greeting()},"
                }
                Text {
                    style = Styles.homeName
                    accessibilityRole = "header"
                    +displayName
                }
                Text {
                    style = Styles.homeEmail
                    +props.account.email
                }
            }
        }
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.scrollContent
            View {
                style = Styles.disclaimer
                View {
                    style = Styles.alertIcon
                    alertIcon(Palette.WARNING_TEXT)
                }
                Text {
                    style = Styles.disclaimerText
                    +"O Recife Saudável não realiza diagnóstico médico e não substitui atendimento profissional. Em emergência, ligue 192 (SAMU)."
                }
            }
            unavailableAction?.let { message ->
                errorAlert(message)
            }
            View {
                style = Styles.cardCompact
                Text {
                    style = Styles.eyebrow
                    accessibilityRole = "header"
                    +"Ações rápidas"
                }
                View {
                    style = Styles.quickGrid
                    View {
                        style = Styles.quickRow
                        quickAction(
                            title = "Pré-triagem",
                            subtitle = "Informe sintomas",
                            green = false,
                            onPress = ::markUnavailable,
                        ) { clipboardIcon(it) }
                        quickAction(
                            title = "Agendar",
                            subtitle = "Escolha data e hora",
                            green = true,
                            onPress = ::markUnavailable,
                        ) { calendarIcon(it) }
                    }
                    Pressable {
                        style = pressable(Styles.quickWide)
                        accessibilityRole = "button"
                        onPress = { markUnavailable() }
                        View {
                            style = arrayOf(Styles.quickIcon, Styles.quickIconPurple)
                            listIcon(Palette.PURPLE_TEXT)
                        }
                        View {
                            style = Styles.quickCopy
                            Text {
                                style = Styles.quickTitle
                                +"Minhas consultas"
                            }
                            Text {
                                style = Styles.quickSubtitle
                                +"Ver e gerenciar agendamentos"
                            }
                        }
                        chevronIcon(Palette.INK)
                    }
                }
            }
            View {
                style = Styles.cardCompact
                View {
                    style = Styles.sectionRow
                    Text {
                        style = Styles.eyebrowInline
                        accessibilityRole = "header"
                        +"Próximas consultas"
                    }
                    textLink("Ver todas", onPress = ::markUnavailable)
                }
                Text {
                    style = Styles.fieldHint
                    +upcoming
                }
            }
        }
        AppFooter()
    }
}

private fun ChildrenBuilder.quickAction(
    title: String,
    subtitle: String,
    green: Boolean,
    onPress: () -> Unit,
    icon: ChildrenBuilder.(color: String) -> Unit,
) {
    Pressable {
        style = pressable(Styles.quickTile, if (green) Styles.quickTileGreen else null)
        accessibilityRole = "button"
        this.onPress = onPress
        View {
            style = arrayOf(Styles.quickIcon, if (green) Styles.quickIconGreen else null)
            icon(this, if (green) Palette.GREEN_TEXT else Palette.BLUE)
        }
        View {
            Text {
                style = Styles.quickTitle
                +title
            }
            Text {
                style = Styles.quickSubtitle
                +subtitle
            }
        }
    }
}

private fun greeting(): String {
    val hour = Date().getHours()
    return when {
        hour < 12 -> "Bom dia"
        hour < 18 -> "Boa tarde"
        else -> "Boa noite"
    }
}
