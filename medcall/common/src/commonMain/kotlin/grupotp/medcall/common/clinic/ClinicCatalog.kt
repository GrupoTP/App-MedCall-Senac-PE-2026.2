package grupotp.medcall.common.clinic

import grupotp.medcall.common.scheduling.Clinic
import grupotp.medcall.common.scheduling.Professional
import grupotp.medcall.common.scheduling.Specialty
import kotlinx.serialization.Serializable

/** The signed-in clinic, with what its filters can choose from. */
@Serializable
data class ClinicCatalog(
    val clinic: Clinic,
    val specialties: List<Specialty>,
    val professionals: List<Professional>,
)

