package grupotp.medcall.admin

import grupotp.medcall.common.staff.StaffRole
import web.location.location

/** Screens of the portals. The path is the URL hash, so nginx needs no route of its own. */
enum class Page(
    val path: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val roles: Set<StaffRole>,
    val section: String,
    /** Shorter name for the side menu. */
    val menuTitle: String = title,
) {
    ClinicDashboard(
        "/clinica/dashboard", "Indicadores da clínica",
        "Acompanhe a agenda e decida onde agir. Todos os blocos seguem os filtros.",
        "bi-speedometer2", setOf(StaffRole.ClinicAdmin), "Portal da Clínica", "Indicadores",
    ),
    ClinicAgenda(
        "/clinica/agenda", "Agenda", "Agendamentos feitos pelo app MedCall: responda, remarque e registre o comparecimento.",
        "bi-calendar-week", setOf(StaffRole.ClinicAdmin, StaffRole.Receptionist), "Portal da Clínica",
    ),
    ClinicTeam(
        "/clinica/equipe", "Equipe da clínica", "Administradores e recepcionistas com acesso ao portal.",
        "bi-people", setOf(StaffRole.ClinicAdmin), "Portal da Clínica",
    ),
    ClinicIntegrations(
        "/clinica/integracoes", "Integrações", "Webhooks e chaves de API para o sistema da clínica falar com o MedCall.",
        "bi-plug", setOf(StaffRole.ClinicAdmin), "Portal da Clínica",
    ),
    ClinicAudit(
        "/clinica/auditoria", "Registro de ações", "Quem fez o quê no portal da clínica.",
        "bi-journal-text", setOf(StaffRole.ClinicAdmin), "Portal da Clínica",
    ),
    MedcallOverview(
        "/medcall/visao-geral", "Visão geral da plataforma", "Indicadores agregados de todas as clínicas. Nenhum dado de paciente.",
        "bi-graph-up-arrow", setOf(StaffRole.MedcallAdmin), "Painel MedCall", "Visão geral",
    ),
    MedcallClinics(
        "/medcall/clinicas", "Clínicas parceiras", "Cadastro, situação e indicadores de cada clínica.",
        "bi-hospital", setOf(StaffRole.MedcallAdmin), "Painel MedCall", "Clínicas",
    ),
    MedcallProfessionals(
        "/medcall/profissionais", "Profissionais e especialidades", "Quem atende em cada clínica.",
        "bi-person-badge", setOf(StaffRole.MedcallAdmin), "Painel MedCall", "Profissionais",
    ),
    MedcallUsers(
        "/medcall/usuarios", "Usuários das clínicas", "Contas do Portal da Clínica de todas as clínicas.",
        "bi-person-gear", setOf(StaffRole.MedcallAdmin), "Painel MedCall",
    ),
    MedcallAudit(
        "/medcall/auditoria", "Registro de ações", "Ações administrativas em toda a plataforma.",
        "bi-journal-text", setOf(StaffRole.MedcallAdmin), "Painel MedCall",
    ),
    ;

    companion object {
        /** First screen of each role. */
        fun home(role: StaffRole): Page = when (role) {
            StaffRole.MedcallAdmin -> MedcallOverview
            StaffRole.ClinicAdmin -> ClinicDashboard
            StaffRole.Receptionist -> ClinicAgenda
        }
    }
}

/** The page and the rest of the path, such as a clinic id in `/medcall/clinicas/3`. */
data class Route(val page: Page, val argument: String? = null)

object Router {
    fun current(role: StaffRole): Route {
        val hash = location.hash.removePrefix("#")
        val page = Page.entries.filter { hash == it.path || hash.startsWith(it.path + "/") }
            .maxByOrNull { it.path.length }
        if (page == null || role !in page.roles) {
            return Route(Page.home(role))
        }
        val argument = hash.removePrefix(page.path).removePrefix("/").ifEmpty { null }
        return Route(page, argument)
    }

    fun go(page: Page, argument: String? = null) {
        location.hash = "#" + page.path + (argument?.let { "/$it" } ?: "")
    }

    fun href(page: Page, argument: String? = null): String = "#" + page.path + (argument?.let { "/$it" } ?: "")
}
