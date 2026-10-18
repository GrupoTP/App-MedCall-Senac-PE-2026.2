package grupotp.recifesaudavel.app.ui.component.layout

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.json
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import ojaynico.kotlin.react.native.safearea.useSafeAreaInsets
import react.FC
import react.Props

/** Shown at the bottom of the screens and on the welcome screen. */
internal const val FOOTER_TEXT =
    "Copyright © 2026, Polyana Fontes; Thayná Batista da Silva; App Recife Saudável, Senac PE. Todos os direitos reservados."

internal val AppFooter = FC<Props> {
    val insets = useSafeAreaInsets()
    View {
        style = arrayOf(Styles.appFooter, json { paddingBottom = 12 + insets.bottom })
        Text {
            style = Styles.appFooterText
            +FOOTER_TEXT
        }
    }
}
