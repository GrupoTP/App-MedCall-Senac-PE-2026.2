package grupotp.medcall.admin

/** Settings from `config.js`, loaded by index.html before the bundle; nothing environment-specific is compiled in. */
object AppConfig {
    private val raw: dynamic get() = js("globalThis.MEDCALL_ADMIN_CONFIG")

    /** API address without a trailing slash; empty means the portals' own origin. */
    val apiBaseUrl: String
        get() = (raw?.apiBaseUrl as? String)?.trim()?.trimEnd('/').orEmpty()
}
