package grupotp.recifesaudavel.worker.routing

import grupotp.recifesaudavel.common.offline.LocalWorkerApi
import grupotp.recifesaudavel.worker.shell.ShellFiles
import web.url.URL

/**
 * Decides what the service worker may do with a request.
 * Server responses, including health data, are never stored in the app-shell cache.
 *
 * @param origin the worker's own origin, such as `http://localhost:8081`. Requests to any
 * other origin, the API server included, are left to the browser: only the page's own files
 * are cached, and only the page itself can reach the local database.
 */
class WorkerRequestRouter(private val origin: String) {
    fun route(url: String): WorkerRoute {
        val parsed = URL(url)
        if (parsed.origin != origin) {
            return WorkerRoute.Ignore
        }
        val path = parsed.pathname
        return when {
            isServerData(path) -> WorkerRoute.Server
            path == LocalWorkerApi.ACCOUNT_PATH -> WorkerRoute.LocalAccount
            path == LocalWorkerApi.APPOINTMENTS_PATH -> WorkerRoute.LocalAppointments
            path == LocalWorkerApi.PENDING_SIGN_OUTS_PATH -> WorkerRoute.LocalPendingSignOuts
            path in ShellFiles.DOCUMENT_PATHS -> WorkerRoute.Document
            ShellFiles.isShellFile(path) -> WorkerRoute.Shell
            else -> WorkerRoute.Ignore
        }
    }

    /** API routes served from the page's own origin, as behind a reverse proxy. */
    private fun isServerData(path: String): Boolean =
        path.startsWith("/api/") || path == "/healthz" || path == "/readyz"
}
