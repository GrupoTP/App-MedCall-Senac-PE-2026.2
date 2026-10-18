package grupotp.recifesaudavel.common.scheduling

import kotlinx.serialization.Serializable

/**
 * Free times at one clinic. Clinics in different zones are never merged: 09:30 in Recife and
 * 09:30 in Manaus are different moments.
 *
 * @property days in the clinic's own dates and times.
 */
@Serializable
data class ClinicAvailability(
    val clinic: Clinic,
    val days: List<AvailableDay>,
)
