package grupotp.medcall.server.domain

import grupotp.medcall.common.api.ErrorCode

/**
 * A refusal decided by a service. Services only name the [code]; the text shown to the person
 * comes from [ErrorMessages][grupotp.medcall.server.i18n.ErrorMessages], filled with [arguments].
 */
data class ServiceError(
    val code: ErrorCode,
    val arguments: List<Any> = emptyList(),
) {
    constructor(code: ErrorCode, vararg arguments: Any) : this(code, arguments.toList())
}
