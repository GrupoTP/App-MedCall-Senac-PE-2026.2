package grupotp.recifesaudavel.server.service

import grupotp.recifesaudavel.common.pretriage.PreTriage

sealed interface PreTriageResult {
    data class Created(val preTriage: PreTriage) : PreTriageResult

    data class Rejected(val error: ServiceError) : PreTriageResult
}
