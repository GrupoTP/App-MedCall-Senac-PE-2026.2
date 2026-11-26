package grupotp.medcall.admin.api

import grupotp.medcall.common.api.ErrorCode

/** A refusal of the server, with the text it sent for the person. */
class ApiException(val status: Int, val code: ErrorCode?, override val message: String) : Exception(message)

