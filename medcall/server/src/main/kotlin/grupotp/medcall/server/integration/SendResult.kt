package grupotp.medcall.server.integration

/** Result of one POST. [statusCode] is null when the request never got an answer. */
data class SendResult(val statusCode: Int?, val durationMs: Long, val error: String = "") {
    val delivered: Boolean get() = statusCode != null && statusCode in 200..299
}

