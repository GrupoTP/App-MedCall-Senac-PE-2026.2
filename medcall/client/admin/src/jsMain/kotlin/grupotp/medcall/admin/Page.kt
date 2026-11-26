package grupotp.medcall.admin

import grupotp.medcall.common.staff.StaffRole

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
    MedcallIntegrations(
        "/medcall/integracoes", "Integrações das clínicas",
        "Webhooks de todas as clínicas: configure para quem não tem equipe de TI e acompanhe as entregas.",
        "bi-plug", setOf(StaffRole.MedcallAdmin), "Painel MedCall", "Integrações",
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

