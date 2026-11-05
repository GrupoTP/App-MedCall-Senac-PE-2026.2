@file:JsModule("expo-constants")

package grupotp.recifesaudavel.app.platform

/**
 * `Constants` from expo-constants, the default export.
 */
@JsName("default")
internal external val expoConstants: ExpoConstants

internal external interface ExpoConstants {
    val expoConfig: ExpoConfig?
}

internal external interface ExpoConfig {
    /** Metro host and port, such as `192.168.0.10:8081`. Only set when the bundle comes from Metro. */
    val hostUri: String?

    /** `extra` of app.config.js. */
    val extra: ExpoExtra?
}

internal external interface ExpoExtra {
    /** `API_BASE_URL` when the bundle was made, or empty. */
    val apiBaseUrl: String?
}
