package grupotp.recifesaudavel.app.ui.component.text

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import react.ChildrenBuilder

/**
 * The prototype draws its notices with emoji, so the icons here are emoji too.
 */
internal fun ChildrenBuilder.emoji(symbol: String, style: dynamic = Styles.emoji) {
    Text {
        this.style = style
        accessible = false
        +symbol
    }
}
