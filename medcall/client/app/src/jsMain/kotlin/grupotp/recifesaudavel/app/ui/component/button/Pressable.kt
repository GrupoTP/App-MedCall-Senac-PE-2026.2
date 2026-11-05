package grupotp.recifesaudavel.app.ui.component.button

import grupotp.recifesaudavel.app.ui.theme.Styles

/**
 * Pressable style that shrinks slightly while pressed, like the prototype :active rule.
 */
internal fun pressable(vararg styles: dynamic): (dynamic) -> dynamic = { state ->
    if (state.pressed == true) arrayOf(*styles, Styles.pressed) else arrayOf(*styles)
}
