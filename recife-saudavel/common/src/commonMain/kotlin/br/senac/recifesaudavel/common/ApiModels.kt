package br.senac.recifesaudavel.common

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
)

@Serializable
data class SessionResponse(
    val token: String,
    val user: UserResponse,
)

@Serializable
data class ErrorResponse(
    val message: String,
)
