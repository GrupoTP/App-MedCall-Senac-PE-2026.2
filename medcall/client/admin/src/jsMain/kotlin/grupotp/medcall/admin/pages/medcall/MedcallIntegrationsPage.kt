package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.admin.pages.clinic.WebhookAccess
import grupotp.medcall.admin.pages.clinic.WebhooksSection
import grupotp.medcall.admin.useSession
import grupotp.medcall.admin.ui.idFilter
import grupotp.medcall.admin.ui.loading
import grupotp.medcall.common.admin.ManagedClinic
import react.FC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.i
import react.useEffectOnce
import react.useState
import web.cssom.ClassName

val MedcallIntegrationsPage = FC {
    val session = useSession()
    var clinics by useState<List<ManagedClinic>?>(null)
    var clinicFilter by useState<Int?>(null)
    useEffectOnce {
        clinics = runCatching { session.api.clinics() }.getOrDefault(emptyList())
    }

    div {
        className = ClassName("alert alert-light border d-flex gap-3 align-items-start mb-4")
        i { className = ClassName("bi bi-info-circle fs-4 text-primary") }
        div {
            className = ClassName("small")
            +"Para clínicas e consultórios sem equipe de TI, a equipe MedCall pode cadastrar e testar o webhook por aqui. "
            +"Cada ação fica no Registro de ações da clínica, com o seu nome. As chaves de API continuam com a própria clínica, "
            +"e as entregas mostram só o tipo de evento e a resposta HTTP, nunca dados de pacientes."
        }
    }
    val list = clinics
    if (list == null) {
        loading()
        return@FC
    }
    div {
        className = ClassName("row mb-3")
        idFilter("integrations-clinic", "Clínica", "Todas as clínicas", list.map { it.id to it.name }, clinicFilter) { clinicFilter = it }
    }
    WebhooksSection {
        access = WebhookAccess.medcall(session.api, clinicFilter)
        this.clinics = list.map { it.id to it.name }
        this.clinicFilter = clinicFilter
    }
}

