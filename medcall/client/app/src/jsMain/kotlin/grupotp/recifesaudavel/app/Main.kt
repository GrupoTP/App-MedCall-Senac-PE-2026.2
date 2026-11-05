@file:OptIn(ExperimentalJsExport::class)

package grupotp.recifesaudavel.app

import grupotp.recifesaudavel.app.di.appModule
import grupotp.recifesaudavel.app.platform.ensureSecureRandom
import grupotp.recifesaudavel.app.platform.loadTimeZones
import ojaynico.kotlin.react.native.api.Platform
import org.koin.core.context.startKoin

private val root: dynamic by lazy {
    ensureSecureRandom()
    loadTimeZones()
    startKoin {
        modules(appModule(Platform.OS))
    }
    Root
}

/**
 * Root component for the Expo shell, which registers it on Android and on the web.
 */
@JsExport
fun createApp(): dynamic = root

fun main() {
    // Referencing the export here keeps it in the webpack bundle.
    createApp()
}
