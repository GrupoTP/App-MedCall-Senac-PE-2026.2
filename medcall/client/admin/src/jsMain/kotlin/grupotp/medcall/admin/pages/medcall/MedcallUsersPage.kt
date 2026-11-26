package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.pages.StaffTable
import grupotp.medcall.admin.pages.resetResult
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.common.staff.StaffUserWithPassword
import kotlinx.coroutines.launch
import react.FC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.p
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.navigator.navigator

val MedcallUsersPage = FC {
    val session = useSession()
    var users by useState<List<StaffUser>?>(null)
    var clinics by useState<List<ManagedClinic>>(emptyList())
    var reload by useState(0)
    var reset by useState<StaffUserWithPassword?>(null)
    useEffect(reload) {
        users = runCatching { session.api.allClinicStaff() }.getOrNull()
        clinics = runCatching { session.api.clinics() }.getOrDefault(emptyList())
    }
    card(
        title = "Todas as contas do Portal da Clínica",
        question = "Para incidentes: desative uma conta ou gere uma senha nova. A gestão do dia a dia é da própria clínica.",
        bodyClass = "card-body p-0",
    ) {
        val list = users
        if (list == null) {
            loading()
        } else {
            StaffTable {
                this.users = list
                me = session.account
                clinicNames = clinics.associate { it.id to it.name }
                onActive = { user, active ->
                    appScope.launch {
                        try {
                            session.api.setUserActive(user.id, active)
                            session.notify(if (active) "${user.fullName} reativado." else "${user.fullName} desativado.", false)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        }
                        reload++
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
    resetResult(reset, { navigator.clipboard.writeTextAsync(it); session.notify("Senha copiada.", false) }) { reset = null }
    p { className = ClassName("small text-secondary"); +"O cadastro de um novo acesso fica na página da clínica (Clínicas › nome da clínica)." }
}

