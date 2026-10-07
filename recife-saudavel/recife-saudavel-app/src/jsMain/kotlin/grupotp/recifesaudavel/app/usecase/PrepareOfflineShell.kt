package grupotp.recifesaudavel.app.usecase

class PrepareOfflineShell(
    private val shell: AppShellGateway,
) {
    suspend operator fun invoke() {
        shell.prepare()
    }
}
