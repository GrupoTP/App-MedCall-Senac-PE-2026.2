package br.senac.recifesaudavel.common

fun LoginRequest.validationError(): String? {
    if (email.isBlank()) {
        return "Informe o e-mail."
    }
    if (password.isBlank()) {
        return "Informe a senha."
    }
    return null
}
