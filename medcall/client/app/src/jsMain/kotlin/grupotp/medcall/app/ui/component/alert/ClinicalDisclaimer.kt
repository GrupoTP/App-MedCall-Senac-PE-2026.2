package grupotp.recifesaudavel.app.ui.component.alert

import grupotp.recifesaudavel.app.ui.icon.alertIcon
import grupotp.recifesaudavel.app.ui.theme.Palette
import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder

/**
 * The prototype ClinicalDisclaimer: the app never diagnoses and points to SAMU.
 */
internal fun ChildrenBuilder.clinicalDisclaimer() {
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
}
