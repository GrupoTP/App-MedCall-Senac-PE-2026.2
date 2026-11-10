package grupotp.medcall.server.integration

import com.sun.net.httpserver.HttpServer
import grupotp.medcall.common.clinic.AppointmentEventType
import grupotp.medcall.common.integration.WebhookEvent
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebhookSenderTest {
    private val received = mutableListOf<Pair<Map<String, String>, String>>()
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
        createContext("/hook") { exchange ->
            val headers = exchange.requestHeaders.mapValues { it.value.first() }.mapKeys { it.key.lowercase() }
            received += headers to exchange.requestBody.readBytes().decodeToString()
            exchange.sendResponseHeaders(if (headers["x-medcall-event"] == "fail") 500 else 204, -1)
            exchange.close()
        }
        start()
    }
    private val url = "http://127.0.0.1:${server.address.port}/hook"

    @AfterTest
    fun stop() = server.stop(0)

    @Test
    fun theBodyIsSignedSoTheClinicCanCheckIt() {
        // Arrange
        val sender = WebhookSender(allowPrivateTargets = true)

        // Act
        val result = sender.send(url, """{"a":1}""", "appointment.created", "evt-1", listOf("whsec_novo", "whsec_antigo"), 1_700_000_000)

        // Assert
        assertTrue(result.delivered)
        val (headers, body) = received.single()
        assertEquals("evt-1", headers["x-medcall-event-id"])
        val expected = "t=1700000000,v1=" + WebhookSender.hmac("whsec_novo", "1700000000.$body") +
            ",v1=" + WebhookSender.hmac("whsec_antigo", "1700000000.$body")
        assertEquals(expected, headers["x-medcall-signature"])
    }

    @Test
    fun failuresAreReportedWithTheirStatus() {
        val result = WebhookSender(allowPrivateTargets = true).send(url, "{}", "fail", "evt-2", listOf("s"), 1)

        assertEquals(500, result.statusCode)
        assertTrue(!result.delivered)
    }

    @Test
    fun internalAddressesAreRefusedInProduction() {
        val sender = WebhookSender(allowPrivateTargets = false)

        assertNotNull(sender.urlProblem(url))
        assertNotNull(sender.urlProblem("https://169.254.169.254/latest/meta-data"))
        assertNotNull(sender.urlProblem("https://192.168.0.10/hook"))
        assertNotNull(sender.urlProblem("ftp://example.com"))
        assertNull(sender.urlProblem("https://8.8.8.8/hook"))
    }

    @Test
    fun eachWebhookGetsTheMostSpecificEventItSubscribedTo() {
        val all = WebhookEvent.entries
        assertEquals(WebhookEvent.AppointmentCreated, IntegrationService.webhookEventFor(AppointmentEventType.Created, all))
        assertEquals(WebhookEvent.AppointmentCancelledByPatient, IntegrationService.webhookEventFor(AppointmentEventType.Cancelled, all))
        assertEquals(
            WebhookEvent.AppointmentStatusChanged,
            IntegrationService.webhookEventFor(AppointmentEventType.Cancelled, listOf(WebhookEvent.AppointmentStatusChanged)),
        )
        assertEquals(WebhookEvent.AppointmentStatusChanged, IntegrationService.webhookEventFor(AppointmentEventType.Confirmed, all))
        assertNull(IntegrationService.webhookEventFor(AppointmentEventType.Created, listOf(WebhookEvent.AppointmentStatusChanged)))
    }
}
