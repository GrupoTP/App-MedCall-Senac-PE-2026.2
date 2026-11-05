package grupotp.recifesaudavel.app.ui.screen.pretriage

import grupotp.recifesaudavel.app.ui.component.alert.warnAlert
import grupotp.recifesaudavel.app.ui.component.button.primaryButton
import grupotp.recifesaudavel.app.ui.component.button.secondaryButton
import grupotp.recifesaudavel.app.ui.component.layout.AppFooter
import grupotp.recifesaudavel.app.ui.icon.largeCheckCircleIcon
import grupotp.recifesaudavel.app.ui.theme.Palette
import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.ScrollView
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.FC
import react.Props

internal external interface PreTriageSuccessScreenProps : Props {
    var onSchedule: () -> Unit
    var onHome: () -> Unit
}

/**
 * Shown only after the server stored the pre-triage.
 */
internal val PreTriageSuccessScreen = FC<PreTriageSuccessScreenProps> { props ->
    val insets = useSafeAreaInsets()
    View {
        style = Styles.screen
        ScrollView {
            style = Styles.scroll
            contentContainerStyle = Styles.successScroll
            View {
                style = arrayOf(Styles.successInner, json { paddingTop = 32 + insets.top })
                View {
                    style = arrayOf(Styles.successIcon, Styles.successIconGreen)
                    largeCheckCircleIcon(Palette.CONFIRMED_TEXT)
                }
                View {
                    style = Styles.successHeading
                    Text {
                        style = Styles.successTitle
                        accessibilityRole = "header"
                        +"Informações registradas!"
                    }
                    Text {
                        style = Styles.successSubtitle
                        +"Suas respostas foram salvas e serão consideradas na preparação do atendimento."
                    }
                }
                warnAlert(
                    "O Recife Saudável não realiza diagnóstico. A pré-triagem será vinculada à próxima " +
                        "consulta que você agendar. Aguarde o contato da clínica ou agende agora.",
                )
                View {
                    style = Styles.formActions
                    primaryButton("Agendar consulta agora", onPress = props.onSchedule)
                    secondaryButton("Voltar ao início", onPress = props.onHome)
                }
            }
        }
        AppFooter()
    }
}
