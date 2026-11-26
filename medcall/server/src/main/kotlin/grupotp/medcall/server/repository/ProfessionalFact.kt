package grupotp.medcall.server.repository

data class ProfessionalFact(
    val id: Int,
    val name: String,
    val clinicId: Int,
    val specialtyId: Int,
    val specialtyName: String,
    val active: Boolean,
)

