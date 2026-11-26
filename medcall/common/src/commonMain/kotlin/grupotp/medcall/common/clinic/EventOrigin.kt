package grupotp.medcall.common.clinic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EventOrigin {
    @SerialName("patient_app") PatientApp,
    @SerialName("clinic_portal") ClinicPortal,
    @SerialName("public_api") PublicApi,
    @SerialName("demo_seed") DemoSeed,
}

