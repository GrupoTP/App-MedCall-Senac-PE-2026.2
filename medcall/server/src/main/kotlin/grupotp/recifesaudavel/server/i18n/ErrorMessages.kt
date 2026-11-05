package grupotp.recifesaudavel.server.i18n

import grupotp.recifesaudavel.common.api.ErrorCode
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

/**
 * Texts of the [ErrorCode]s, read from the `i18n/errors.properties` bundle. The bundle is in
 * Brazilian Portuguese, the only language of the app; another language would be a sibling file
 * such as `errors_en.properties`.
 */
object ErrorMessages {
    private val locale: Locale = Locale.forLanguageTag("pt-BR")
    private val bundle: ResourceBundle = ResourceBundle.getBundle(
        "i18n.errors",
        locale,
        ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES),
    )

    fun of(code: ErrorCode, arguments: List<Any> = emptyList()): String =
        MessageFormat(bundle.getString(code.name), locale).format(arguments.toTypedArray())
}
