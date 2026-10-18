package grupotp.recifesaudavel.app.ui.font

import grupotp.recifesaudavel.app.ui.theme.FontFamily
import ojaynico.kotlin.react.json

/**
 * Registers Nunito and Poppins. The prototype only uses the regular weight.
 * Returns true once the fonts are ready, or when loading failed and the system font takes over.
 */
internal fun useAppFonts(): Boolean {
    val (loaded, error) = useFonts(
        json {
            this[FontFamily.BODY] = Nunito_400Regular
            this[FontFamily.DISPLAY] = Poppins_400Regular
        },
    )
    return loaded == true || error != null
}
