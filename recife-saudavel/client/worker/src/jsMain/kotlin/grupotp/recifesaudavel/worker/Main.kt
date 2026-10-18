package grupotp.recifesaudavel.worker

import grupotp.recifesaudavel.worker.local.LocalAccountHandler
import grupotp.recifesaudavel.worker.local.LocalAppointmentsHandler
import grupotp.recifesaudavel.worker.local.PendingSignOutsHandler
import grupotp.recifesaudavel.worker.routing.WorkerRequestRouter
import grupotp.recifesaudavel.worker.routing.WorkerRoute
import grupotp.recifesaudavel.worker.shell.DocumentHandler
import grupotp.recifesaudavel.worker.shell.ShellAssetHandler
import grupotp.recifesaudavel.worker.shell.ShellCache
import grupotp.recifesaudavel.worker.storage.LocalAccountDatabase
import kotlinx.serialization.json.Json
import web.serviceworker.ServiceWorkerModule

/** Builds the worker's parts and starts listening to the browser's events. */
fun main() {
    ServiceWorkerModule { self ->
        val json = Json { ignoreUnknownKeys = true }
        val shell = ShellCache()
        val database = LocalAccountDatabase(json)
        OfflineWorker(
            self = self,
            shell = shell,
            router = WorkerRequestRouter(origin = self.location.origin),
            handlers = mapOf(
                WorkerRoute.Document to DocumentHandler(shell),
                WorkerRoute.Shell to ShellAssetHandler(shell),
                WorkerRoute.LocalAccount to LocalAccountHandler(database, json),
                WorkerRoute.LocalAppointments to LocalAppointmentsHandler(database, json),
                WorkerRoute.LocalPendingSignOuts to PendingSignOutsHandler(database, json),
            ),
        ).start()
    }
}
