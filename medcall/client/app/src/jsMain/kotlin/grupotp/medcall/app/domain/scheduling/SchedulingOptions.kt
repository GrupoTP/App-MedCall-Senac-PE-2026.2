package grupotp.medcall.app.domain.scheduling

import grupotp.medcall.common.scheduling.Clinic
import grupotp.medcall.common.scheduling.Professional
import grupotp.medcall.common.scheduling.Specialty

/**
 * Choices of the first scheduling step.
 */
data class SchedulingOptions(
    val specialties: List<Specialty>,
    val professionals: List<Professional>,
    val clinics: List<Clinic>,
) {
    fun professionalsOf(specialtyId: Int): List<Professional> = professionals.filter { it.specialtyId == specialtyId }

    fun clinic(id: Int?): Clinic? = clinics.firstOrNull { it.id == id }
}
