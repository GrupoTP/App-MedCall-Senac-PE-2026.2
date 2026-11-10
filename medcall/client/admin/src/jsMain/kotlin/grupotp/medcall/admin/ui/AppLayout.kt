package grupotp.medcall.admin.ui

import grupotp.medcall.admin.Page
import grupotp.medcall.admin.Route
import grupotp.medcall.admin.Router
import grupotp.medcall.admin.SessionContext
import grupotp.medcall.admin.pages.AuditPage
import grupotp.medcall.admin.pages.clinic.ClinicAgendaPage
import grupotp.medcall.admin.pages.clinic.ClinicDashboardPage
import grupotp.medcall.admin.pages.clinic.ClinicIntegrationsPage
import grupotp.medcall.admin.pages.ClinicTeamPage
import grupotp.medcall.admin.pages.medcall.MedcallClinicsPage
import grupotp.medcall.admin.pages.medcall.MedcallOverviewPage
import grupotp.medcall.admin.pages.medcall.MedcallProfessionalsPage
import grupotp.medcall.admin.pages.medcall.MedcallUsersPage
import grupotp.medcall.common.staff.StaffRole
import kotlinx.coroutines.delay
import react.FC
import react.Key
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.aside
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.footer
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.ul
import react.useEffect
import grupotp.medcall.admin.useSession
import react.useState
import web.cssom.ClassName
import web.dom.document
import web.window.window

external interface AppLayoutProps : Props {
    var route: Route
    var hash: String
    var pendingTick: Int
    var onSignOut: () -> Unit
}

/** AdminLTE 4 shell: header, sidebar with the role's menu, page title and content. */
val AppLayout = FC<AppLayoutProps> { props ->
    val session = useSession()
    val account = session.account
    val page = props.route.page
    var pending by useState<Int?>(null)

    // Pending answers, on the Agenda item; read again every minute and after each action.
    useEffect(props.pendingTick, account.id) {
        if (!account.role.isClinicRole) return@useEffect
        while (true) {
            pending = runCatching { session.api.pendingCount() }.getOrNull()
            delay(60_000)
        }
        @Suppress("UNREACHABLE_CODE")
        Unit
    }
    useEffect(page) {
        document.title = "${page.title} · MedCall"
        closeSidebarOnMobile()
    }

    div {
        className = ClassName("app-wrapper")
        nav {
            className = ClassName("app-header navbar navbar-expand bg-body")
            div {
                className = ClassName("container-fluid")
                ul {
                    className = ClassName("navbar-nav")
                    li {
                        className = ClassName("nav-item")
                        button {
                            type = Kinds.button
                            className = ClassName("nav-link btn btn-link")
                            asDynamic()["aria-label"] = "Mostrar ou esconder o menu"
                            onClick = { toggleSidebar() }
                            i { className = ClassName("bi bi-list fs-5") }
                        }
                    }
                    li {
                        className = ClassName("nav-item d-none d-md-block")
                        span {
                            className = ClassName("nav-link text-secondary")
                            +(account.clinicName ?: "Equipe MedCall")
                        }
                    }
                }
                ul {
                    className = ClassName("navbar-nav ms-auto align-items-center")
                    li {
                        className = ClassName("nav-item d-none d-sm-block me-2 text-end lh-sm")
                        strong { className = ClassName("d-block small"); +account.fullName }
                        span { className = ClassName("small text-secondary"); +Format.role(account.role) }
                    }
                    li {
                        className = ClassName("nav-item")
                        button {
                            type = Kinds.button
                            className = ClassName("btn btn-sm btn-outline-secondary")
                            onClick = { props.onSignOut() }
                            i { className = ClassName("bi bi-box-arrow-right me-1") }
                            +"Sair"
                        }
                    }
                }
            }
        }
        aside {
            className = ClassName("app-sidebar shadow")
            asDynamic()["data-bs-theme"] = "dark"
            div {
                className = ClassName("sidebar-brand")
                a {
                    className = ClassName("brand-link")
                    href = Router.href(Page.home(account.role))
                    img { className = ClassName("brand-image"); src = "medcall-symbol.svg"; alt = "MedCall" }
                    span { className = ClassName("brand-text fw-semibold"); +"MedCall" }
                }
            }
            div {
                className = ClassName("sidebar-wrapper")
                nav {
                    className = ClassName("mt-2")
                    ul {
                        className = ClassName("nav sidebar-menu flex-column")
                        asDynamic().role = "menu"
                        val pages = Page.entries.filter { account.role in it.roles }
                        pages.groupBy { it.section }.forEach { (section, items) ->
                            li { className = ClassName("nav-header"); +section.uppercase() }
                            items.forEach { item ->
                                li {
                                    key = Key(item.name)
                                    className = ClassName("nav-item")
                                    a {
                                        className = ClassName("nav-link d-flex align-items-center${if (item == page) " active" else ""}")
                                        href = Router.href(item)
                                        i { className = ClassName("nav-icon bi ${item.icon}") }
                                        p { +item.menuTitle }
                                        val count = pending
                                        if (item == Page.ClinicAgenda && count != null && count > 0) {
                                            span {
                                                className = ClassName("nav-badge badge text-bg-warning ms-auto")
                                                title = "Agendamentos aguardando resposta"
                                                +count.toString()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        div {
            className = ClassName("sidebar-overlay")
            onClick = { closeSidebarOnMobile() }
        }
        main {
            className = ClassName("app-main")
            div {
                className = ClassName("app-content-header")
                div {
                    className = ClassName("container-fluid")
                    h3 { className = ClassName("mb-0"); +page.title }
                    div { className = ClassName("subtitle"); +page.subtitle }
                }
            }
            div {
                className = ClassName("app-content")
                div {
                    className = ClassName("container-fluid pb-4")
                    when (page) {
                        Page.ClinicDashboard -> ClinicDashboardPage()
                        Page.ClinicAgenda -> ClinicAgendaPage()
                        Page.ClinicTeam -> ClinicTeamPage()
                        Page.ClinicIntegrations -> ClinicIntegrationsPage()
                        Page.ClinicAudit, Page.MedcallAudit -> AuditPage()
                        Page.MedcallOverview -> MedcallOverviewPage()
                        Page.MedcallClinics -> MedcallClinicsPage { clinicId = props.route.argument?.toIntOrNull() }
                        Page.MedcallProfessionals -> MedcallProfessionalsPage()
                        Page.MedcallUsers -> MedcallUsersPage()
                    }
                }
            }
        }
        footer {
            className = ClassName("app-footer small text-secondary")
            +"MedCall · Projeto Integrador ADS Senac 2026 · "
            +(if (account.role == StaffRole.MedcallAdmin) "Painel de Controle" else "Portal da Clínica")
        }
    }
}

/** AdminLTE's JS is not loaded: React owns the DOM, so the sidebar toggle is done here. */
private fun toggleSidebar() {
    val body = document.body
    if (window.innerWidth >= 992) {
        body.classList.toggle(ClassName("sidebar-collapse"))
    } else {
        body.classList.toggle(ClassName("sidebar-open"))
    }
}

private fun closeSidebarOnMobile() {
    document.body.classList.remove(ClassName("sidebar-open"))
}
