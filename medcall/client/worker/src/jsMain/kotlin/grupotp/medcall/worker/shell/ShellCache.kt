package grupotp.recifesaudavel.worker.shell

import grupotp.recifesaudavel.worker.http.WorkerResponses
import js.promise.await
import web.cache.Cache
import web.cache.add
import web.cache.caches
import web.cache.delete
import web.cache.keys
import web.cache.match
import web.cache.open
import web.cache.put
import web.http.Request
import web.http.Response
import web.http.fetchAsync
import web.http.text

/**
 * Keeps only the files needed to open the interface.
 * Callers must not pass server or health responses.
 */
class ShellCache {
    suspend fun install() {
        val cache = open()
        for (path in ShellFiles.ESSENTIAL_PATHS) {
            cache.add(path)
        }
        val page = cache.match(ShellFiles.ROOT_DOCUMENT) ?: return
        for (path in ExportedPage(page.text()).bundlePaths()) {
            cache.add(path)
        }
    }

    suspend fun deleteRetiredCaches() {
        for (name in caches.keys()) {
            if (name != NAME) {
                caches.delete(name)
            }
        }
    }

    suspend fun match(key: String): Response? = open().match(key)

    /** Fetches [request] and, when it succeeds, keeps a copy under [key]. Throws when offline. */
    suspend fun fetchAndStore(request: Request, key: String): Response {
        val fresh = fetchAsync(request).await()
        if (fresh.ok) {
            open().put(key, fresh.clone())
        }
        return fresh
    }

    /** The stored page, or a short notice when it was never stored. */
    suspend fun storedDocument(): Response = match(ShellFiles.ROOT_DOCUMENT) ?: WorkerResponses.offline()

    private suspend fun open(): Cache = caches.open(NAME)

    private companion object {
        const val NAME = "recife-saudavel-shell-v3"
    }
}
