package grupotp.medcall.admin.pages.clinic

import grupotp.medcall.admin.api.AdminApi
import grupotp.medcall.common.integration.SaveWebhookRequest
import grupotp.medcall.common.integration.Webhook
import grupotp.medcall.common.integration.WebhookDelivery
import grupotp.medcall.common.integration.WebhookTestResult
import grupotp.medcall.common.integration.WebhookWithSecret

/**
 * The webhook actions of one portal: the clinic admin's own, or the Painel MedCall's, which
 * reaches every clinic. The screens are the same; only the routes and the clinic column change.
 */
class WebhookAccess(
    val list: suspend () -> List<Webhook>,
    val deliveries: suspend () -> List<WebhookDelivery>,
    /** The clinic is null in the clinic portal (the server uses the admin's own). */
    val create: suspend (clinicId: Int?, request: SaveWebhookRequest) -> WebhookWithSecret,
    val update: suspend (id: Int, request: SaveWebhookRequest) -> Webhook,
    val rotateSecret: suspend (id: Int) -> WebhookWithSecret,
    val test: suspend (id: Int) -> WebhookTestResult,
    val redeliver: suspend (id: Int) -> WebhookDelivery,
) {
    companion object {
        fun clinic(api: AdminApi) = WebhookAccess(
            list = { api.webhooks() },
            deliveries = { api.deliveries(null) },
            create = { _, request -> api.createWebhook(request) },
            update = api::updateWebhook,
            rotateSecret = api::rotateWebhookSecret,
            test = api::testWebhook,
            redeliver = api::redeliver,
        )

        fun medcall(api: AdminApi, clinicFilter: Int?) = WebhookAccess(
            list = { api.medcallWebhooks(clinicFilter) },
            deliveries = { api.medcallDeliveries(clinicFilter) },
            create = { clinicId, request -> api.medcallCreateWebhook(checkNotNull(clinicId), request) },
            update = api::medcallUpdateWebhook,
            rotateSecret = api::medcallRotateWebhookSecret,
            test = api::medcallTestWebhook,
            redeliver = api::medcallRedeliver,
        )
    }
}

