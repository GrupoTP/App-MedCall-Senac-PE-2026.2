package grupotp.medcall.app.usecase.offline

import grupotp.medcall.app.gateway.local.AppShellGateway

class PrepareOfflineShellUseCase(
    private val shell: AppShellGateway,
) {
    suspend operator fun invoke() {
        shell.prepare()
    }
}
