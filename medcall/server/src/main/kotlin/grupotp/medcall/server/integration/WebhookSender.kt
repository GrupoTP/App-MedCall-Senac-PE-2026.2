package grupotp.medcall.server.integration

import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Result of one POST. [statusCode] is null when the request never got an answer. */
data class SendResult(val statusCode: Int?, val durationMs: Long, val error: String = "") {
    val delivered: Boolean get() = statusCode != null && statusCode in 200..299
}

/**
 * Posts webhook bodies to clinics' systems.
 *
 * Signature (documented in the public API docs): `X-MedCall-Signature: t=<unix seconds>,v1=<hex>`,
 * where hex is HMAC-SHA256 with the webhook secret over `"<t>.<body>"`. During 24 hours after a
 * rotation a second `v1` with the old secret goes along, so the clinic can switch without losing
 * events. The timestamp lets the clinic refuse replays.
 */
class WebhookSender(
    private val allowPrivateTargets: Boolean = false,
) {
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    /**
     * Why [url] cannot receive webhooks, or null when it can: only HTTPS to public addresses, so
     * a clinic can never make the server call its own network (SSRF).
     */
    fun urlProblem(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return "URL inválida"
        if (uri.scheme != "https" && !(allowPrivateTargets && uri.scheme == "http")) return "Use HTTPS"
        val host = uri.host ?: return "URL sem host"
        if (allowPrivateTargets) return null
        val addresses = runCatching { InetAddress.getAllByName(host).toList() }.getOrNull() ?: return "Host não encontrado"
        return if (addresses.any(::isInternal)) "Endereço interno" else null
    }

    fun send(url: String, body: String, eventType: String, eventId: String, secrets: List<String>, nowSeconds: Long): SendResult {
        // Checked again at send time: the name may now point somewhere else.
        urlProblem(url)?.let { return SendResult(null, 0, it) }
        val signature = "t=$nowSeconds," + secrets.joinToString(",") { "v1=" + hmac(it, "$nowSeconds.$body") }
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .header("Content-Type", "application/json")
            .header("User-Agent", "MedCall-Webhooks/1.0")
            .header("X-MedCall-Event", eventType)
            .header("X-MedCall-Event-Id", eventId)
            .header("X-MedCall-Signature", signature)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val started = System.nanoTime()
        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.discarding())
            SendResult(response.statusCode(), elapsed(started))
        } catch (error: Exception) {
            SendResult(null, elapsed(started), error.javaClass.simpleName + (error.message?.let { ": $it" } ?: ""))
        }
    }

    private fun elapsed(started: Long) = (System.nanoTime() - started) / 1_000_000

    companion object {
        const val TIMEOUT_SECONDS = 10L

        fun hmac(secret: String, data: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
            return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
        }

        /** Loopback, private, link-local (cloud metadata), unique-local and the like. */
        fun isInternal(address: InetAddress): Boolean =
            address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress ||
                address.isAnyLocalAddress || address.isMulticastAddress ||
                (address is Inet6Address && (address.address[0].toInt() and 0xfe) == 0xfc) ||
                address.hostAddress.startsWith("100.64.") // carrier-grade NAT
    }
}
