package br.senac.recifesaudavel.ui

// The phone reaches the computer API through `adb reverse tcp:8080 tcp:8080`,
// so Android uses the same localhost address as the browser.
fun apiBaseUrl(): String = "http://localhost:$API_PORT"

private const val API_PORT = 8080
