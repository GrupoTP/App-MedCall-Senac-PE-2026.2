package grupotp.medcall.server.integration

/** A clinic system authenticated by an API key. */
data class ApiPrincipal(val apiKeyId: Int, val clinicId: Int)

