@file:OptIn(ExperimentalJsExport::class)

package br.senac.recifesaudavel.ui

@JsExport
fun createApp(): dynamic = App

fun main() {
    // The Expo shell calls createApp() and registers the component.
    // Referencing the export here keeps it in the webpack bundle.
    createApp()
}
