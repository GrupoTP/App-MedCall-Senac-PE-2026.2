package grupotp.recifesaudavel.app.domain.offline

/**
 * Installs the offline shell so the interface can open without a network.
 */
interface AppShellGateway {
    suspend fun prepare()
}
