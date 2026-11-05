@file:JsModule("react-native")

package grupotp.recifesaudavel.app.platform

/**
 * `Linking` from react-native. Opens a URL with the app registered for it, such as the mail app for `mailto:`.
 */
internal external object Linking {
    fun openURL(url: String): dynamic
}
