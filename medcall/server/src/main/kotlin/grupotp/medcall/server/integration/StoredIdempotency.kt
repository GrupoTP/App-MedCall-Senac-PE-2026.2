package grupotp.medcall.server.integration

data class StoredIdempotency(val requestHash: String, val statusCode: Int, val body: String)

