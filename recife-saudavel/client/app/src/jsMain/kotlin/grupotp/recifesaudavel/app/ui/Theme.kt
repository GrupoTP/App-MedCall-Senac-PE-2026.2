package grupotp.recifesaudavel.app.ui

/**
 * Colors from the Figma prototype. Same names the web stylesheet used.
 */
internal object Palette {
    const val BLUE = "#2d7dd2"
    const val BLUE_DARK = "#1b5ea8"
    const val BLUE_HOVER = "#2468b2"
    const val INK = "#1c2b3a"
    const val MUTED = "#5a7a94"
    const val LINE = "#c8dff0"
    const val CANVAS = "#eef4fa"
    const val SOFT = "#b8d8f5"
    const val SOFTER = "#8abfe6"
    const val FOOTER = "#6aacd5"
    const val FOOTER_ON_WHITE = "#8aaec8"
    const val GREETING = "#a8ceed"
    const val WHATSAPP = "#25d366"
    const val WHITE = "#ffffff"
    const val REQUIRED = "#ef4444"

    const val ERROR_TEXT = "#dc2626"
    const val ERROR_BORDER = "#fecaca"
    const val ERROR_BACKGROUND = "#fef2f2"

    const val WARNING_TEXT = "#b45309"
    const val WARNING_BORDER = "#fde68a"
    const val WARNING_BACKGROUND = "#fffbeb"

    const val GREEN_TEXT = "#1e6b5e"
    const val GREEN_BORDER = "#a8d5c8"
    const val GREEN_BACKGROUND = "#e8f5f2"

    const val PURPLE_TEXT = "#7b5ea7"
    const val PURPLE_BORDER = "#c4b8e0"
    const val PURPLE_BACKGROUND = "#f0ebf8"
    const val GREEN_ICON_BACKGROUND = "rgba(30, 107, 94, 0.1)"
    const val PURPLE_ICON_BACKGROUND = "rgba(123, 94, 167, 0.1)"
    const val BLUE_ICON_BACKGROUND = "rgba(45, 125, 210, 0.1)"

    // Tailwind green/yellow/red used by the prototype for steps and appointment status.
    const val STEP_TEXT = "#3d5c72"
    const val SUCCESS_TEXT = "#166534"
    const val SUCCESS_BORDER = "#86efac"
    const val SUCCESS_BACKGROUND = "#dcfce7"
    const val CONFIRMED_TEXT = "#15803d"
    const val PENDING_TEXT = "#a16207"
    const val PENDING_BACKGROUND = "#fef9c3"
    const val CANCELLED_TEXT = "#ef4444"
    const val CANCELLED_BACKGROUND = "#fee2e2"
}

/**
 * Font families registered by [useAppFonts].
 */
internal object FontFamily {
    const val BODY = "Nunito"
    const val DISPLAY = "Poppins"
}

/** Widest column for the welcome screen (28rem). */
internal const val WELCOME_MAX_WIDTH = 448

/** Widest column for the inner screens (48rem). */
internal const val CONTENT_MAX_WIDTH = 768
