package grupotp.medcall.admin.pages.medcall

import grupotp.medcall.common.kpi.ClinicStatus
import react.ChildrenBuilder
import react.dom.html.ReactHTML.span
import web.cssom.ClassName

internal fun ChildrenBuilder.statusPill(status: ClinicStatus) {
    span {
        className = ClassName(
            "badge " + when (status) {
                ClinicStatus.Active -> "text-bg-success"
                ClinicStatus.Draft -> "text-bg-warning"
                ClinicStatus.Inactive -> "text-bg-secondary"
            },
        )
        +when (status) {
            ClinicStatus.Active -> "ativa"
            ClinicStatus.Draft -> "rascunho"
            ClinicStatus.Inactive -> "inativa"
        }
    }
}

