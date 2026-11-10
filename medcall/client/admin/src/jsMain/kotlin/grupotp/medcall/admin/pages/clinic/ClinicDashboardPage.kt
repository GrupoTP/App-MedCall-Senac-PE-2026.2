package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.Page
import grupotp.medcall.admin.Router
import grupotp.medcall.admin.SessionContext
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.api.PeriodChoice
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.ui.Format
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.downloadText
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.idFilter
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.periodFilter
import grupotp.medcall.common.clinic.ClinicCatalog
import grupotp.medcall.common.kpi.ClinicDashboard
import kotlinx.coroutines.launch
import react.FC
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.span
import react.useEffect
import react.useEffectOnce
import grupotp.medcall.admin.useSession
import react.useState
import web.cssom.ClassName

/** Portal da Clínica: the administrator's dashboard (SPM-87, 2ª VA de Data Science). */
val ClinicDashboardPage = FC {
    val session = useSession()
    var catalog by useState<ClinicCatalog?>(null)
    var period by useState(PeriodChoice(30))
    var specialtyId by useState<Int?>(null)
    var professionalId by useState<Int?>(null)
    var data by useState<ClinicDashboard?>(null)
    var error by useState<String?>(null)
    var reload by useState(0)
    var exporting by useState(false)

    useEffectOnce {
        catalog = runCatching { session.api.clinicCatalog() }.getOrNull()
    }
    useEffect(period, specialtyId, professionalId, reload) {
        error = null
        try {
            data = session.api.clinicDashboard(period, specialtyId, professionalId)
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }

    div {
        className = ClassName("filter-bar row g-2 align-items-end mb-4 mx-0")
        periodFilter(period) { period = it }
        val professionals = catalog?.professionals.orEmpty().filter { specialtyId == null || it.specialtyId == specialtyId }
        idFilter("filter-specialty", "Especialidade", "Todas", catalog?.specialties.orEmpty().map { it.id to it.name }, specialtyId) {
            specialtyId = it
            professionalId = null
        }
        idFilter("filter-professional", "Profissional", "Todos", professionals.map { it.id to it.name }, professionalId) { professionalId = it }
        div {
            className = ClassName("col-12 col-lg ms-lg-auto text-lg-end")
            button {
                type = Kinds.button
                className = ClassName("btn btn-outline-primary")
                disabled = exporting
                title = "Base usada no painel, sem dados de pacientes (CSV com ponto e vírgula)"
                onClick = {
                    exporting = true
                    appScope.launch {
                        try {
                            val csv = session.api.downloadCsv(period)
                            val current = data
                            val name = if (current != null) "medcall-agendamentos-${current.from}-a-${current.to}.csv" else "medcall-agendamentos.csv"
                            downloadText(name, csv)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        } finally {
                            exporting = false
                        }
                    }
                }
                i { className = ClassName("bi bi-download me-1") }
                +"Exportar base (CSV)"
            }
        }
    }

    errorAlert(error) { reload++ }
    val current = data
    if (current == null) {
        if (error == null) loading("Calculando os indicadores…")
        return@FC
    }
    div {
        className = ClassName("small text-secondary mb-2")
        i { className = ClassName("bi bi-calendar3 me-1") }
        +"${Format.date(current.from)} a ${Format.date(current.to)}"
        span { className = ClassName("ms-2"); +"· passe o mouse sobre um indicador para ver a definição" }
    }
    clinicDashboardBody(current, pendingHref = Router.href(Page.ClinicAgenda, "pendentes"))
}
