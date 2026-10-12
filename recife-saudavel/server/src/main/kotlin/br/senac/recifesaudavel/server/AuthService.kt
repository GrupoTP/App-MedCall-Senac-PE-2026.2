package br.senac.recifesaudavel.server

import br.senac.recifesaudavel.common.LoginRequest
import br.senac.recifesaudavel.common.SessionResponse
import br.senac.recifesaudavel.common.UserResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal object MockUser {
    const val ID = "maria"
    const val NAME = "Maria da Silva"
    const val EMAIL = "maria@recife.saude"
    const val PASSWORD = "saudavel"
}

class AuthService {
    private val sessions = ConcurrentHashMap<String, UserResponse>()

    fun login(request: LoginRequest): SessionResponse? {
        val emailMatches = request.email.trim() == MockUser.EMAIL
        val passwordMatches = request.password == MockUser.PASSWORD
        if (!emailMatches || !passwordMatches) {
            return null
        }

        val user = UserResponse(
            id = MockUser.ID,
            name = MockUser.NAME,
            email = MockUser.EMAIL,
        )
        val token = UUID.randomUUID().toString()
        sessions[token] = user
        return SessionResponse(token = token, user = user)
    }

    fun current(token: String?): SessionResponse? {
        if (token.isNullOrBlank()) {
            return null
        }
        val user = sessions[token] ?: return null
        return SessionResponse(token = token, user = user)
    }

    fun logout(token: String?): Boolean {
        if (token.isNullOrBlank()) {
            return false
        }
        return sessions.remove(token) != null
    }
}
