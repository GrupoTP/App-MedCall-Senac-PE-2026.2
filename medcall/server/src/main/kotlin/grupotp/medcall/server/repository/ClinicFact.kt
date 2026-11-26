package grupotp.medcall.server.repository

import grupotp.medcall.common.kpi.ClinicStatus

data class ClinicFact(
    val id: Int,
    val name: String,
    val city: String,
    val timeZoneId: String,
    val status: ClinicStatus,
    val hasActiveWebhook: Boolean,
)

