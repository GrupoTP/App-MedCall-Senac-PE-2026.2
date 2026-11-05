package grupotp.recifesaudavel.worker.http

import web.http.Request
import web.http.Response

/** Answers one kind of request the worker takes over from the browser. */
fun interface RequestHandler {
    suspend fun handle(request: Request): Response
}
