package grupotp.recifesaudavel.app.usecase.offline

import grupotp.recifesaudavel.app.gateway.local.AppShellGateway

class PrepareOfflineShellUseCase(
    private val shell: AppShellGateway,
) {
    suspend operator fun invoke() {
        shell.prepare()
    }
}
