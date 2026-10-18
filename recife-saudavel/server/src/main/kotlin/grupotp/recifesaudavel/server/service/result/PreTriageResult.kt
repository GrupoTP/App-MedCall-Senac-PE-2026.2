package grupotp.recifesaudavel.server.service.result

import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.server.domain.ServiceError

sealed interface PreTriageResult {
    data class Created(val preTriage: PreTriage) : PreTriageResult

    data class Rejected(val error: ServiceError) : PreTriageResult
}
