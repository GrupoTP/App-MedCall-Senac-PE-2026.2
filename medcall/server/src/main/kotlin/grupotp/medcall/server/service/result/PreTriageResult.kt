package grupotp.medcall.server.service.result

import grupotp.medcall.common.pretriage.PreTriage
import grupotp.medcall.server.domain.ServiceError

sealed interface PreTriageResult {
    data class Created(val preTriage: PreTriage) : PreTriageResult

    data class Rejected(val error: ServiceError) : PreTriageResult
}
