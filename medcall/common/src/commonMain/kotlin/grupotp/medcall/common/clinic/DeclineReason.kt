package grupotp.medcall.common.clinic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Fixed list, so the dashboard can count them. */
@Serializable
enum class DeclineReason(val label: String) {
    @SerialName("professional_unavailable") ProfessionalUnavailable("Profissional indisponível"),
    @SerialName("clinic_closed") ClinicClosed("Clínica fechada no dia"),
    @SerialName("not_covered") NotCovered("Atendimento não coberto pela clínica"),
    @SerialName("duplicate") Duplicate("Agendamento em duplicidade"),
    @SerialName("other") Other("Outro motivo"),
}

