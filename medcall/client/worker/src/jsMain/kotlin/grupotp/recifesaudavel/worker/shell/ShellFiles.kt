package grupotp.recifesaudavel.worker.shell

import web.url.URL

/** Files of the app shell: the page and what it needs to open without a connection. */
object ShellFiles {
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
    private val BUILD_OUTPUT_PREFIXES = listOf("/_expo/static/", "/assets/")

    fun isShellFile(path: String): Boolean = path in ESSENTIAL_PATHS || isBuildOutput(path)

    /**
     * Key used to store a shell response. Fixed files use their path; build output keeps the query,
     * because Metro tells assets apart by it.
     */
    fun cacheKey(url: String): String {
        val path = URL(url).pathname
        return if (isBuildOutput(path)) url.substringBefore("#") else path
    }

    private fun isBuildOutput(path: String): Boolean = BUILD_OUTPUT_PREFIXES.any(path::startsWith)
}
