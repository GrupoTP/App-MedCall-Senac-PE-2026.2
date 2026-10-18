package grupotp.recifesaudavel.worker.shell

import grupotp.recifesaudavel.worker.http.RequestHandler
import web.http.Request
import web.http.Response

/** The page: from the network first, so a new deploy reaches the device; the stored copy offline. */
class DocumentHandler(private val shell: ShellCache) : RequestHandler {
    override suspend fun handle(request: Request): Response = try {
        shell.fetchAndStore(request, ShellFiles.ROOT_DOCUMENT)
    } catch (_: Throwable) {
        shell.storedDocument()
    }
}
