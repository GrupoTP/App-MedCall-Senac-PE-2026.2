package grupotp.recifesaudavel.app.ui

import grupotp.recifesaudavel.app.usecase.ListUpcomingAppointments
import grupotp.recifesaudavel.app.usecase.UpcomingAppointmentsResult
import grupotp.recifesaudavel.common.AuthenticatedAccount
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.useState
import web.html.ButtonType
import kotlin.js.Date

internal external interface HomeScreenProps : Props {
    var account: AuthenticatedAccount
    var listUpcomingAppointments: ListUpcomingAppointments
}

internal val HomeScreen = FC<HomeScreenProps> { props ->
    var unavailableAction by useState<String?>(null)
    val upcoming = when (props.listUpcomingAppointments()) {
        UpcomingAppointmentsResult.NotImplemented -> NOT_IMPLEMENTED_MESSAGE
    }
    val displayName = props.account.socialName.ifBlank { props.account.fullName }

    fun markUnavailable() {
        unavailableAction = NOT_IMPLEMENTED_MESSAGE
    }

    div {
        className = classNames("screen")
        div {
            className = classNames("home-header")
            div {
                className = classNames("home-header-inner")
                div {
                    className = classNames("home-header-tools")
                    button {
                        type = buttonKind
                        className = classNames("home-icon-button")
                        ariaLabel = "Configurações"
                        onClick = { markUnavailable() }
                        settingsIcon()
                    }
                }
                p {
                    className = classNames("home-greeting")
                    +"${greeting()},"
                }
                h2 {
                    className = classNames("home-name")
                    +displayName
                }
                p {
                    className = classNames("home-email")
                    +props.account.email
                }
            }
        }
        div {
            className = classNames("screen-scroll")
            div {
                className = classNames("screen-content")
                div {
                    className = classNames("disclaimer")
                    alertIcon()
                    p {
                        +"O Recife Saudável não realiza diagnóstico médico e não substitui atendimento profissional. Em emergência, ligue 192 (SAMU)."
                    }
                }
                unavailableAction?.let { message ->
                    errorAlert(message)
                }
                div {
                    className = classNames("card card-compact")
                    p {
                        className = classNames("eyebrow")
                        +"Ações rápidas"
                    }
                    div {
                        className = classNames("quick-grid")
                        quickAction(
                            title = "Pré-triagem",
                            subtitle = "Informe sintomas",
                            tone = "blue",
                            onClick = { markUnavailable() },
                        ) { clipboardIcon() }
                        quickAction(
                            title = "Agendar",
                            subtitle = "Escolha data e hora",
                            tone = "green",
                            onClick = { markUnavailable() },
                        ) { calendarIcon() }
                        button {
                            type = buttonKind
                            className = classNames("quick-wide")
                            onClick = { markUnavailable() }
                            span {
                                className = classNames("quick-icon purple")
                                listIcon()
                            }
                            div {
                                className = classNames("quick-copy")
                                p {
                                    className = classNames("quick-title")
                                    +"Minhas consultas"
                                }
                                p {
                                    className = classNames("quick-subtitle")
                                    +"Ver e gerenciar agendamentos"
                                }
                            }
                            chevronIcon()
                        }
                    }
                }
                div {
                    className = classNames("card card-compact")
                    div {
                        className = classNames("section-row")
                        p {
                            className = classNames("eyebrow")
                            +"Próximas consultas"
                        }
                        button {
                            type = buttonKind
                            className = classNames("text-link")
                            onClick = { markUnavailable() }
                            +"Ver todas"
                        }
                    }
                    p {
                        className = classNames("field-hint")
                        +upcoming
                    }
                }
            }
        }
        appFooter()
    }
}

private fun react.ChildrenBuilder.quickAction(
    title: String,
    subtitle: String,
    tone: String,
    onClick: () -> Unit,
    icon: react.ChildrenBuilder.() -> Unit,
) {
    button {
        type = buttonKind
        className = classNames("quick-tile", tone)
        this.onClick = { onClick() }
        span {
            className = classNames("quick-icon", tone)
            icon()
        }
        div {
            p {
                className = classNames("quick-title")
                +title
            }
            p {
                className = classNames("quick-subtitle")
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
