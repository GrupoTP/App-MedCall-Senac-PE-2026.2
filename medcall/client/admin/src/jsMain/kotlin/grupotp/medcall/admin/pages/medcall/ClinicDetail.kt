package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.admin.Page
import grupotp.medcall.admin.Router
import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.api.PeriodChoice
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.pages.NewUserDialog
import grupotp.medcall.admin.pages.StaffTable
import grupotp.medcall.admin.pages.clinic.clinicDashboardBody
import grupotp.medcall.admin.pages.resetResult
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.periodFilter
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.kpi.ClinicDashboard
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.common.staff.StaffUserWithPassword
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.navigator.navigator

external interface ClinicDetailProps : Props {
    var clinicId: Int
}


/** One clinic in the panel: its KPIs (masked) and its team, never its appointments. */
internal val ClinicDetail = FC<ClinicDetailProps> { props ->
    val session = useSession()
    var clinic by useState<ManagedClinic?>(null)
    var period by useState(PeriodChoice(30))
    var dashboard by useState<ClinicDashboard?>(null)
    var team by useState<List<StaffUser>?>(null)
    var error by useState<String?>(null)
    var reloadTeam by useState(0)
    var creating by useState(false)
    var reset by useState<StaffUserWithPassword?>(null)

    useEffect(props.clinicId) {
        clinic = runCatching { session.api.clinics().firstOrNull { it.id == props.clinicId } }.getOrNull()
    }
    useEffect(props.clinicId, period) {
        try {
            dashboard = session.api.clinicDashboardForMedcall(props.clinicId, period)
            error = null
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }
    useEffect(props.clinicId, reloadTeam) {
        team = runCatching { session.api.clinicTeam(props.clinicId) }.getOrNull()
    }

    div {
        className = ClassName("d-flex flex-wrap align-items-center gap-2 mb-3")
        a { className = ClassName("btn btn-sm btn-outline-secondary"); href = Router.href(Page.MedcallClinics); i { className = ClassName("bi bi-arrow-left me-1") }; +"Clínicas" }
        val current = clinic
        if (current != null) {
            h4 { className = ClassName("mb-0 ms-2"); +current.name }
            statusPill(current.status)
            span { className = ClassName("text-secondary small"); +"${current.city} · ${current.timeZoneId}" }
        }
    }
    card(
        title = "Equipe da clínica",
        question = "A MedCall cria o primeiro administrador; depois a própria clínica cadastra a equipe no portal.",
        tools = {
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary btn-sm")
                onClick = { creating = true }
                i { className = ClassName("bi bi-person-plus me-1") }
                +"Novo acesso"
            }
        },
        bodyClass = "card-body p-0",
    ) {
        val list = team
        if (list == null) {
            loading()
        } else {
            StaffTable {
                users = list
                me = session.account
                onActive = { user, active ->
                    appScope.launch {
                        try {
                            session.api.setUserActive(user.id, active)
                            session.notify(if (active) "${user.fullName} reativado." else "${user.fullName} desativado.", false)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        }
                        reloadTeam++
                    }
                }
                onReset = { user ->
                    appScope.launch {
                        try {
                            reset = session.api.resetPasswordByMedcall(user.id)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        }
                    }
                }
            }
        }
    }
    div {
        className = ClassName("filter-bar row g-2 align-items-end mb-4 mx-0")
        periodFilter(period) { period = it }
        div {
            className = ClassName("col-12 col-lg text-lg-end small text-secondary")
            i { className = ClassName("bi bi-shield-lock me-1") }
            +"Indicadores agregados da clínica. A MedCall não vê a lista de agendamentos nem dados de pacientes."
        }
    }
    errorAlert(error)
    val current = dashboard
    if (current == null) {
        if (error == null) loading()
    } else {
        clinicDashboardBody(current, pendingHref = null)
    }
    if (creating) {
        NewUserDialog {
            title = "Novo acesso para ${clinic?.name ?: "a clínica"}"
            roles = listOf(StaffRole.ClinicAdmin, StaffRole.Receptionist)
            onCreate = { request -> session.api.createUserForClinic(props.clinicId, request).also { reloadTeam++ } }
            onClose = { creating = false }
        }
    }
    resetResult(reset, { navigator.clipboard.writeTextAsync(it); session.notify("Senha copiada.", false) }) { reset = null }
}

