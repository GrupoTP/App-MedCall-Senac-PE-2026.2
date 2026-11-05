package grupotp.recifesaudavel.worker.shell

import grupotp.recifesaudavel.worker.http.RequestHandler
import web.http.Request
import web.http.Response

/** Files the interface needs: the stored copy first, the network only the first time. */
class ShellAssetHandler(private val shell: ShellCache) : RequestHandler {
    override suspend fun handle(request: Request): Response {
        val key = ShellFiles.cacheKey(request.url)
        return shell.match(key) ?: try {
            shell.fetchAndStore(request, key)
        } catch (_: Throwable) {
            shell.storedDocument()
        }
    }
}
