package grupotp.recifesaudavel.app.gateway.local

/**
 * Installs the offline shell so the interface can open without a network.
 */
interface AppShellGateway {
    suspend fun prepare()
}
