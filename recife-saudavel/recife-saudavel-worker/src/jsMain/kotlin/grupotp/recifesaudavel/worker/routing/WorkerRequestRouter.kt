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
            path in DOCUMENT_PATHS -> WorkerRoute.Document
            path in essentialPaths || isBuildOutput(path) -> WorkerRoute.Shell
            else -> WorkerRoute.Ignore
        }
    }

    /**
     * Key used to store a shell response. Fixed files use their path; build output keeps the query,
     * because Metro tells assets apart by it.
     */
    fun cacheKey(url: String): String {
        val path = pathOf(url)
        return if (isBuildOutput(path)) url.substringBefore("#") else path
    }

    private fun isServerData(url: String, path: String): Boolean {
        if (path.startsWith("/api/") || path == "/healthz" || path == "/readyz") {
            return true
        }
        val host = hostOf(url)
        return host.endsWith(":8080")
    }

    private fun isBuildOutput(path: String): Boolean = BUILD_OUTPUT_PREFIXES.any(path::startsWith)

    companion object {
        const val LOCAL_ACCOUNT_PATH = "/local/account"
        const val SHELL_CACHE_NAME = "recife-saudavel-shell-v3"
        const val ROOT_DOCUMENT = "/"

        /** The page itself. Served from the network first so a new deploy reaches the device. */
        val DOCUMENT_PATHS = setOf(ROOT_DOCUMENT, "/index.html")

        /** Files stored when the worker installs. Every one must exist, or the install fails. */
        val ESSENTIAL_PATHS = setOf(
            ROOT_DOCUMENT,
            "/manifest.json",
            "/icons/icon-192.png",
            "/icons/icon-512.png",
        )

        /** Expo export output. Names carry a content hash, so a stored copy never goes stale. */
        val BUILD_OUTPUT_PREFIXES = listOf("/_expo/static/", "/assets/")

        private val bundleReference = Regex("""(?:src|href)="(/_expo/static/[^"]+)"""")

        /**
         * Build files the exported page loads. The worker installs after the page fetched them,
         * so they are stored on install instead of on first use.
         */
        fun buildOutputIn(document: String): List<String> =
            bundleReference.findAll(document).map { it.groupValues[1] }.distinct().toList()
    }
}

sealed interface WorkerRoute {
    /** The HTML page. Network first, stored copy when offline. */
    data object Document : WorkerRoute

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
