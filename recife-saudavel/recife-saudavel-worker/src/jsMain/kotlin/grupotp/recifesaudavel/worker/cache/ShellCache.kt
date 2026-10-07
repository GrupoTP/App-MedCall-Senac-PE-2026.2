package grupotp.recifesaudavel.worker.cache

import grupotp.recifesaudavel.worker.routing.WorkerRequestRouter
import js.promise.await
import web.cache.caches
import web.http.Response

/**
 * Keeps only the files needed to open the interface.
 * Callers must not pass server or health responses.
 */
class ShellCache {
    suspend fun install() {
        val cache = caches.openAsync(WorkerRequestRouter.SHELL_CACHE_NAME).await()
        for (path in WorkerRequestRouter.ESSENTIAL_PATHS) {
            cache.addAsync(path).await()
        }
    }

    suspend fun deleteRetiredCaches() {
        for (name in caches.keysAsync().await()) {
            if (name != WorkerRequestRouter.SHELL_CACHE_NAME) {
                caches.deleteAsync(name).await()
            }
        }
    }

    suspend fun matchPath(path: String): Response? =
        caches.openAsync(WorkerRequestRouter.SHELL_CACHE_NAME).await().matchAsync(path).await()

    suspend fun store(path: String, response: Response) {
        caches.openAsync(WorkerRequestRouter.SHELL_CACHE_NAME).await().putAsync(path, response).await()
    }
}
