package grupotp.recifesaudavel.app.ui

import react.dom.aria.AriaRole
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span

internal external interface WelcomeScreenProps : Props {
    var onStart: () -> Unit
    var onLogin: () -> Unit
}

private class Feature(
    val title: String,
    val subtitle: String,
    val icon: react.ChildrenBuilder.() -> Unit,
)

private val features = listOf(
    Feature("Pré-triagem informativa", "Registre sintomas com segurança") { triageIcon() },
    Feature("Agendamento simplificado", "Consultas em clínicas parceiras") { calendarIcon() },
    Feature("Jornada acessível", "Menos cliques, mais clareza") { journeyIcon() },
)

internal val WelcomeScreen = FC<WelcomeScreenProps> { props ->
    div {
        className = classNames("welcome")
        div {
            className = classNames("welcome-body")
            div {
                className = classNames("welcome-inner")
                div {
                    className = classNames("logo-badge")
                    heartIcon()
                }
                h1 {
                    className = classNames("welcome-title")
                    +"Recife Saudável"
                }
                p {
                    className = classNames("welcome-tagline")
                    +"Sua saúde na palma da mão."
                }
                p {
                    className = classNames("welcome-subtitle")
                    +"2º Polo Médico do Brasil · Senac PE"
                }
                div {
                    className = classNames("feature-list")
                    role = AriaRole.list
                    ariaLabel = "Funcionalidades do aplicativo"
                    for (feature in features) {
                        div {
                            className = classNames("feature")
                            role = AriaRole.listitem
                            span {
                                className = classNames("feature-icon")
                                ariaHidden = true
                                feature.icon(this)
                            }
                            div {
                                p {
                                    className = classNames("feature-title")
                                    +feature.title
                                }
                                p {
                                    className = classNames("feature-subtitle")
                                    +feature.subtitle
                                }
                            }
                        }
                    }
                }
            }
        }
        div {
            className = classNames("welcome-actions")
            button {
                type = buttonKind
                className = classNames("btn-welcome-primary")
                onClick = { props.onStart() }
                +"Começar"
            }
            button {
                type = buttonKind
                className = classNames("btn-welcome-secondary")
                onClick = { props.onLogin() }
                +"Acessar minha conta"
            }
            p {
                className = classNames("welcome-note")
                +"Acesso seguro · LGPD · Dados protegidos"
            }
            p {
                className = classNames("welcome-footer")
                +FOOTER_TEXT
            }
        }
    }
}
