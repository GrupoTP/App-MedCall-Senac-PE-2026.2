package grupotp.recifesaudavel.worker.routing

/**
 * Decides what the service worker may do with a request.
 * Server responses, including health data, are never stored in the app-shell cache.
 */
class WorkerRequestRouter(
    private val essentialPaths: Set<String> = ESSENTIAL_PATHS,
) {
    fun route(url: String): WorkerRoute {
        val path = pathOf(url)
        return when {
            isServerData(url, path) -> WorkerRoute.Server
            path == LOCAL_ACCOUNT_PATH -> WorkerRoute.LocalAccount
            path in essentialPaths -> WorkerRoute.Shell
            else -> WorkerRoute.Ignore
        }
    }

    private fun isServerData(url: String, path: String): Boolean {
        if (path.startsWith("/api/") || path == "/healthz" || path == "/readyz") {
            return true
        }
        val host = hostOf(url)
        return host.endsWith(":8080")
    }

    companion object {
        const val LOCAL_ACCOUNT_PATH = "/local/account"
        const val SHELL_CACHE_NAME = "recife-saudavel-shell-v2"

        val ESSENTIAL_PATHS = setOf(
            "/",
            "/index.html",
            "/styles.css",
            "/recife-saudavel.js",
            "/manifest.json",
            "/icons/icon-192.png",
            "/icons/icon-512.png",
        )
    }
}

sealed interface WorkerRoute {
    /** Static files required to open the interface. */
    data object Shell : WorkerRoute

    /** Signed-in registration profile kept in the local database. */
    data object LocalAccount : WorkerRoute

    /** API and health responses. Must not be written to the cache. */
    data object Server : WorkerRoute

    /** Leave the request to the browser. */
    data object Ignore : WorkerRoute
}

internal fun pathOf(url: String): String {
    val withoutHash = url.substringBefore("#")
    val withoutQuery = withoutHash.substringBefore("?")
    val schemeEnd = withoutQuery.indexOf("://")
    val pathStart = if (schemeEnd < 0) {
        withoutQuery.indexOf("/")
    } else {
        withoutQuery.indexOf("/", schemeEnd + 3)
    }
    if (pathStart < 0) {
        return "/"
    }
    val path = withoutQuery.substring(pathStart)
    return path.ifBlank { "/" }
}

internal fun hostOf(url: String): String {
    val schemeEnd = url.indexOf("://")
    if (schemeEnd < 0) {
        return ""
    }
    val hostStart = schemeEnd + 3
    val hostEnd = url.indexOf("/", hostStart).let { if (it < 0) url.length else it }
    return url.substring(hostStart, hostEnd)
}
