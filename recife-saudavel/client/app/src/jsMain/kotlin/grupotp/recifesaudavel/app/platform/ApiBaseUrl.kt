package grupotp.recifesaudavel.app.platform

private const val WEB = "web"
private const val API_PORT = 8080

/**
 * `API_BASE_URL`, read by app.config.js when Metro starts or the app is exported, wins.
 *
 * Without it, the server runs on the computer that serves the bundle. Expo Go and development
 * builds load it from Metro, so the API sits on the Metro host. Release builds embed the bundle
 * and the web client must keep the `localhost` origin the server accepts; both call `localhost`,
 * which `adb reverse` forwards to the computer on Android.
 */
fun apiBaseUrl(platform: String): String {
    expoConstants.expoConfig?.extra?.apiBaseUrl?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }?.let { return it }
    val metroHost = if (platform == WEB) null else expoConstants.expoConfig?.hostUri?.substringBefore(':')
    return "http://${metroHost ?: "localhost"}:$API_PORT"
}
