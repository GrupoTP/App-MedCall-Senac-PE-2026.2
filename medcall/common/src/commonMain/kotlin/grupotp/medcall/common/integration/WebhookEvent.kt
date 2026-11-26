package grupotp.medcall.common.integration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Events a clinic can subscribe to. The wire name is what goes in the webhook's `type`. */
@Serializable
enum class WebhookEvent(val wireName: String, val label: String) {
    @SerialName("appointment.created")
    AppointmentCreated("appointment.created", "Novo agendamento"),

    @SerialName("appointment.cancelled_by_patient")
    AppointmentCancelledByPatient("appointment.cancelled_by_patient", "Cancelado pelo paciente"),

    @SerialName("appointment.rescheduled")
    AppointmentRescheduled("appointment.rescheduled", "Remarcado"),

    @SerialName("appointment.status_changed")
    AppointmentStatusChanged("appointment.status_changed", "Qualquer mudança de situação"),
}

