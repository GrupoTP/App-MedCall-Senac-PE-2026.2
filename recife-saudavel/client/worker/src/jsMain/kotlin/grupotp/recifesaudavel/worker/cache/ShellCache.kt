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
        val document = cache.matchAsync(WorkerRequestRouter.ROOT_DOCUMENT).await() ?: return
        for (path in WorkerRequestRouter.buildOutputIn(document.textAsync().await())) {
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

    suspend fun match(key: String): Response? =
        caches.openAsync(WorkerRequestRouter.SHELL_CACHE_NAME).await().matchAsync(key).await()

    suspend fun store(key: String, response: Response) {
        caches.openAsync(WorkerRequestRouter.SHELL_CACHE_NAME).await().putAsync(key, response).await()
    }
}
