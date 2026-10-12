package br.senac.recifesaudavel.ui

import br.senac.recifesaudavel.common.LoginRequest
import br.senac.recifesaudavel.common.SessionResponse
import br.senac.recifesaudavel.common.validationError
import react.FC
import react.useState

private const val TEST_EMAIL = "maria@recife.saude"
private const val TEST_PASSWORD = "saudavel"

internal val App = FC {
    val (session, setSession) = useState<SessionResponse?>(null)
    val (email, setEmail) = useState(TEST_EMAIL)
    val (password, setPassword) = useState(TEST_PASSWORD)
    val (error, setError) = useState<String?>(null)
    val (loading, setLoading) = useState(false)
    val serverUrl = apiBaseUrl()
    val client = SessionClient(serverUrl)
    val current = session

    if (current == null) {
        LoginScreen {
            this.email = email
            this.password = password
            this.error = error
            this.loading = loading
            this.serverUrl = serverUrl
            onEmailChange = { setEmail(it) }
            onPasswordChange = { setPassword(it) }
            onSubmit = submit@{
                if (loading) {
                    return@submit
                }
                val request = LoginRequest(email = email.trim(), password = password)
                val validationMessage = request.validationError()
                if (validationMessage != null) {
                    setError(validationMessage)
                    return@submit
                }
                setLoading(true)
                setError(null)
                client.login(request) { result ->
                    setLoading(false)
                    when (result) {
                        is SessionCall.Success -> setSession(result.session)
                        is SessionCall.Failure -> setError(result.message)
                        SessionCall.LoggedOut -> Unit
                    }
                }
            }
        }
    } else {
        HomeScreen {
            name = current.user.name
            this.email = current.user.email
            this.error = error
            this.loading = loading
            onLogout = logout@{
                if (loading) {
                    return@logout
                }
                setLoading(true)
                setError(null)
                client.logout(current.token) { result ->
                    setLoading(false)
                    when (result) {
                        SessionCall.LoggedOut -> setSession(null)
                        is SessionCall.Failure -> setError(result.message)
                        is SessionCall.Success -> Unit
                    }
                }
            }
        }
    }
}
