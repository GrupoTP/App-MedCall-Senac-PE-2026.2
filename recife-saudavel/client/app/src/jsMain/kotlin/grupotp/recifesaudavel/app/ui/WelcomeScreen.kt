package grupotp.recifesaudavel.app.ui

import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.Pressable
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.ChildrenBuilder
import react.FC
import react.Key
import react.Props

internal external interface WelcomeScreenProps : Props {
    var onStart: () -> Unit
    var onLogin: () -> Unit
}

private class Feature(
    val title: String,
    val subtitle: String,
    val icon: ChildrenBuilder.(color: String) -> Unit,
)

private val features = listOf(
    Feature("Pré-triagem informativa", "Registre sintomas com segurança") { triageIcon(it) },
    Feature("Agendamento simplificado", "Consultas em clínicas parceiras") { calendarIcon(it) },
    Feature("Jornada acessível", "Menos cliques, mais clareza") { journeyIcon(it) },
)

internal val WelcomeScreen = FC<WelcomeScreenProps> { props ->
    val insets = useSafeAreaInsets()
    View {
        style = Styles.welcome
        verticalGradient(id = "welcome-gradient", from = Palette.BLUE, to = Palette.BLUE_DARK)
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.welcomeScrollContent
            View {
                style = arrayOf(Styles.welcomeBody, json { paddingTop = 48 + insets.top })
                View {
                    style = Styles.welcomeInner
                    View {
                        style = Styles.logoBadge
                        heartIcon(Palette.WHITE)
                    }
                    Text {
                        style = Styles.welcomeTitle
                        accessibilityRole = "header"
                        +"Recife Saudável"
                    }
                    Text {
                        style = Styles.welcomeTagline
                        +"Sua saúde na palma da mão."
                    }
                    Text {
                        style = Styles.welcomeSubtitle
                        +"2º Polo Médico do Brasil · Senac PE"
                    }
                    View {
                        style = Styles.featureList
                        accessibilityRole = "list"
                        accessibilityLabel = "Funcionalidades do aplicativo"
                        for (feature in features) {
                            View {
                                key = Key(feature.title)
                                style = Styles.feature
                                View {
                                    style = Styles.featureIcon
                                    feature.icon(this, Palette.WHITE)
                                }
                                View {
                                    style = Styles.featureCopy
                                    Text {
                                        style = Styles.featureTitle
                                        +feature.title
                                    }
                                    Text {
                                        style = Styles.featureSubtitle
                                        +feature.subtitle
                                    }
                                }
                            }
                        }
                    }
                }
            }
            View {
                style = arrayOf(Styles.welcomeActions, json { paddingBottom = 16 + insets.bottom })
                Pressable {
                    style = pressable(Styles.welcomePrimaryButton)
                    accessibilityRole = "button"
                    onPress = props.onStart
                    Text {
                        style = Styles.welcomePrimaryButtonText
                        +"Começar"
                    }
                }
                Pressable {
                    style = pressable(Styles.welcomeSecondaryButton)
                    accessibilityRole = "button"
                    onPress = props.onLogin
                    Text {
                        style = Styles.welcomeSecondaryButtonText
                        +"Acessar minha conta"
                    }
                }
                Text {
                    style = Styles.welcomeNote
                    +"Acesso seguro · LGPD · Dados protegidos"
                }
                Text {
                    style = Styles.welcomeFooter
                    +FOOTER_TEXT
                }
            }
        }
    }
}
