package br.senac.recifesaudavel.common

import kotlinx.serialization.json.Json

val appJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
