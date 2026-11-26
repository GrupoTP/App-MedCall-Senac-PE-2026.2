package grupotp.medcall.admin.pages

import grupotp.medcall.admin.api.ApiException
import grupotp.medcall.admin.appScope
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.Kinds
import grupotp.medcall.admin.ui.card
import grupotp.medcall.admin.ui.errorAlert
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.admin.ui.modal
import grupotp.medcall.admin.ui.secretOnce
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.common.staff.StaffUserWithPassword
import grupotp.medcall.common.staff.UpdateStaffUserRequest
import kotlinx.coroutines.launch
import react.ChildrenBuilder
import react.FC
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.p
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.navigator.navigator

/** Shows a new temporary password after a reset. */
fun ChildrenBuilder.resetResult(result: StaffUserWithPassword?, onCopy: (String) -> Unit, onClose: () -> Unit) {
    if (result == null) return
    modal("Nova senha provisória", onClose, footer = {
        button { type = Kinds.button; className = ClassName("btn btn-primary"); onClick = { onClose() }; +"Pronto" }
    }) {
        p { +"As sessões de ${result.user.fullName} foram encerradas." }
        secretOnce("Senha provisória", result.temporaryPassword.value) { onCopy(result.temporaryPassword.value) }
    }
}


/** Portal da Clínica: the clinic administrator manages the team (SPM-91). */
val ClinicTeamPage = FC {
    val session = useSession()
    var users by useState<List<StaffUser>?>(null)
    var error by useState<String?>(null)
    var reload by useState(0)
    var creating by useState(false)
    var reset by useState<StaffUserWithPassword?>(null)

    useEffect(reload) {
        try {
            users = session.api.clinicUsers()
            error = null
        } catch (failure: ApiException) {
            error = failure.message
        }
        Unit
    }

    fun change(user: StaffUser, request: UpdateStaffUserRequest, message: String) {
        appScope.launch {
            try {
                session.api.updateClinicUser(user.id, request)
                session.notify(message, false)
            } catch (failure: ApiException) {
                session.notify(failure.message, true)
            }
            reload++
        }
    }

    errorAlert(error) { reload++ }
    card(
        title = "Usuários do portal",
        question = "A clínica sempre mantém pelo menos um administrador ativo.",
        tools = {
            button {
                type = Kinds.button
                className = ClassName("btn btn-primary btn-sm")
                onClick = { creating = true }
                i { className = ClassName("bi bi-person-plus me-1") }
                +"Novo usuário"
            }
        },
        bodyClass = "card-body p-0",
    ) {
        val list = users
        if (list == null) {
            loading()
        } else {
            StaffTable {
                this.users = list
                me = session.account
                onRole = { user, role -> change(user, UpdateStaffUserRequest(role = role), "Perfil de ${user.fullName} alterado.") }
                onActive = { user, active ->
                    change(user, UpdateStaffUserRequest(active = active), if (active) "${user.fullName} reativado." else "${user.fullName} desativado; as sessões foram encerradas.")
                }
                onReset = { user ->
                    appScope.launch {
                        try {
                            reset = session.api.resetClinicUserPassword(user.id)
                        } catch (failure: ApiException) {
                            session.notify(failure.message, true)
                        }
                    }
                }
            }
        }
    }
    if (creating) {
        NewUserDialog {
            title = "Novo usuário da clínica"
            roles = listOf(StaffRole.Receptionist, StaffRole.ClinicAdmin)
            onCreate = { request -> session.api.createClinicUser(request).also { reload++ } }
            onClose = { creating = false }
        }
    }
    resetResult(reset, { navigator.clipboard.writeTextAsync(it); session.notify("Senha copiada.", false) }) { reset = null }
}

