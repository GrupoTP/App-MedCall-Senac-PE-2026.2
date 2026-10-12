package br.senac.recifesaudavel.ui

import br.senac.recifesaudavel.common.ErrorResponse
import br.senac.recifesaudavel.common.LoginRequest
import br.senac.recifesaudavel.common.SessionResponse
import br.senac.recifesaudavel.common.appJson
import kotlin.js.Promise

internal sealed interface SessionCall {
    data class Success(val session: SessionResponse) : SessionCall

    data object LoggedOut : SessionCall

    data class Failure(val message: String) : SessionCall
}

internal class SessionClient(
    private val baseUrl: String,
) {
    fun login(request: LoginRequest, onResult: (SessionCall) -> Unit) {
        performRequest(
            url = "$baseUrl/api/sessions",
            method = "POST",
            body = appJson.encodeToString(request),
            token = null,
            onComplete = { status, text ->
                if (status == 200) {
                    onResult(SessionCall.Success(appJson.decodeFromString(text)))
                } else {
                    onResult(SessionCall.Failure(readError(text, "Não foi possível entrar.")))
                }
            },
            onFailure = { message -> onResult(SessionCall.Failure(message)) },
        )
    }

    fun logout(token: String, onResult: (SessionCall) -> Unit) {
        performRequest(
            url = "$baseUrl/api/sessions/current",
            method = "DELETE",
            body = null,
            token = token,
            onComplete = { status, text ->
                if (status == 204 || status == 401) {
                    onResult(SessionCall.LoggedOut)
                } else {
                    onResult(SessionCall.Failure(readError(text, "Não foi possível sair.")))
                }
            },
            onFailure = { message -> onResult(SessionCall.Failure(message)) },
        )
    }
}

private fun readError(text: String, fallback: String): String {
    if (text.isBlank()) {
        return fallback
    }
    return try {
        appJson.decodeFromString<ErrorResponse>(text).message
    } catch (_: Throwable) {
        fallback
    }
}

private fun performRequest(
    url: String,
    method: String,
    body: String?,
    token: String?,
    onComplete: (status: Int, text: String) -> Unit,
    onFailure: (String) -> Unit,
) {
    val headers = js("({})")
    headers["Accept"] = "application/json"
    headers["Content-Type"] = "application/json"
    if (token != null) {
        headers["Authorization"] = "Bearer $token"
    }

    val init = js("({})")
    init.method = method
    init.headers = headers
    if (body != null) {
        init.body = body
    }

    fetch(url, init).then(
        onFulfilled = { response ->
            val textPromise = response.text() as Promise<String>
            textPromise.then(
                onFulfilled = { text ->
                    onComplete((response.status as Number).toInt(), text)
                    null
                },
                onRejected = {
                    onFailure("Não foi possível ler a resposta do servidor.")
                    null
                },
            )
            null
        },
        onRejected = {
            onFailure("Não foi possível conectar ao servidor.")
            null
        },
    )
}

private external fun fetch(input: String, init: dynamic = definedExternally): Promise<dynamic>

private fun Promise<dynamic>.then(
    onFulfilled: (dynamic) -> dynamic,
    onRejected: (dynamic) -> dynamic,
): Promise<dynamic> = asDynamic().then(onFulfilled, onRejected) as Promise<dynamic>
