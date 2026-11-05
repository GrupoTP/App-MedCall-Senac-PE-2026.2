package grupotp.recifesaudavel.app.platform

/**
 * Hermes has no Web Crypto, but `kotlin.uuid.Uuid.random()` calls `crypto.getRandomValues`,
 * and Koin uses it for every module id. Browsers already provide it, so this only fills the gap
 * on Android, with the platform secure random generator from expo-crypto.
 */
internal fun ensureSecureRandom() {
    val global: dynamic = js("globalThis")
    if (global.crypto != null && global.crypto.getRandomValues != null) {
        return
    }
    val crypto: dynamic = global.crypto ?: js("({})")
    crypto.getRandomValues = { typedArray: dynamic -> getRandomValues(typedArray) }
    global.crypto = crypto
}
