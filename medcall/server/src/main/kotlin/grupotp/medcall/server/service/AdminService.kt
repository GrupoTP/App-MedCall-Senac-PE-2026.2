package grupotp.medcall.server.service

import grupotp.medcall.common.admin.ManagedClinic
import grupotp.medcall.common.admin.ManagedProfessional
import grupotp.medcall.common.admin.SaveClinicRequest
import grupotp.medcall.common.admin.SaveProfessionalRequest
import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.kpi.ClinicStatus
import grupotp.medcall.common.scheduling.Specialty
import grupotp.medcall.server.domain.ServiceError
import grupotp.medcall.server.repository.AdminRepository
import grupotp.medcall.server.repository.AuditRepository
import grupotp.medcall.server.security.StaffPrincipal
import java.time.Clock
import java.time.ZoneId
import kotlin.time.toKotlinInstant

/** What MedCall's team manages in the panel: clinics, professionals and specialties. */
class AdminService(
    private val admin: AdminRepository,
    private val audit: AuditRepository,
    private val clock: Clock,
) {
    fun clinics(): List<ManagedClinic> = admin.clinics()

    fun clinic(id: Int): ManagedClinic? = admin.clinic(id)

    fun createClinic(actor: StaffPrincipal, request: SaveClinicRequest): StaffResult<ManagedClinic> {
        validate(request)?.let { return StaffResult.Rejected(it) }
        val clinic = admin.createClinic(request, now())
        audit.record(now(), "clinic.created", clinic.name, clinic.id, actor.staffId)
        return StaffResult.Done(clinic)
    }

    fun updateClinic(actor: StaffPrincipal, id: Int, request: SaveClinicRequest): StaffResult<ManagedClinic> {
        validate(request)?.let { return StaffResult.Rejected(it) }
        val clinic = admin.updateClinic(id, request, now()) ?: return StaffResult.NotFound
        audit.record(now(), "clinic.updated", clinic.name, clinic.id, actor.staffId)
        return StaffResult.Done(clinic)
    }

    /**
     * A clinic only goes active with at least one active professional and one active administrator.
     * Switching it off hides it from new bookings; its appointments stay.
     */
    fun setClinicStatus(actor: StaffPrincipal, id: Int, status: ClinicStatus): StaffResult<ManagedClinic> {
        val clinic = admin.clinic(id) ?: return StaffResult.NotFound
        if (status == ClinicStatus.Active && (clinic.professionals == 0 || clinic.admins == 0)) {
            return StaffResult.Rejected(ServiceError(ErrorCode.CLINIC_NOT_READY))
        }
        admin.setClinicStatus(id, status, now())
        audit.record(now(), "clinic.status.${status.name.lowercase()}", clinic.name, id, actor.staffId)
        return StaffResult.Done(admin.clinic(id)!!)
    }

    fun specialties(): List<Specialty> = admin.specialties()

    fun createSpecialty(actor: StaffPrincipal, name: String): StaffResult<Specialty> {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 100) {
            return StaffResult.Rejected(ServiceError(ErrorCode.NAME_REQUIRED))
        }
        val specialty = admin.createSpecialty(trimmed) ?: return StaffResult.Rejected(ServiceError(ErrorCode.INVALID_CHOICE))
        audit.record(now(), "specialty.created", trimmed, null, actor.staffId)
        return StaffResult.Done(specialty)
    }

    fun professionals(clinicId: Int?): List<ManagedProfessional> = admin.professionals(clinicId)

    fun createProfessional(actor: StaffPrincipal, request: SaveProfessionalRequest): StaffResult<ManagedProfessional> {
        validate(request)?.let { return StaffResult.Rejected(it) }
        val professional = admin.createProfessional(request)
        audit.record(now(), "professional.created", professional.name, professional.clinicId, actor.staffId)
        return StaffResult.Done(professional)
    }

    fun updateProfessional(actor: StaffPrincipal, id: Int, request: SaveProfessionalRequest): StaffResult<ManagedProfessional> {
        validate(request)?.let { return StaffResult.Rejected(it) }
        val professional = admin.updateProfessional(id, request) ?: return StaffResult.NotFound
        audit.record(
            now(),
            if (professional.active) "professional.updated" else "professional.deactivated",
            professional.name,
            professional.clinicId,
            actor.staffId,
        )
        return StaffResult.Done(professional)
    }

    private fun validate(request: SaveClinicRequest): ServiceError? = when {
        request.name.isBlank() || request.name.length > 200 -> ServiceError(ErrorCode.NAME_REQUIRED)
        request.city.isBlank() || request.city.length > 100 -> ServiceError(ErrorCode.NAME_REQUIRED)
        runCatching { ZoneId.of(request.timeZoneId) }.isFailure || !request.timeZoneId.contains('/') ->
            ServiceError(ErrorCode.TIME_ZONE_INVALID)
        request.address.length > 300 || request.phone.length > 40 -> ServiceError(ErrorCode.TEXT_TOO_LONG, 300)
        else -> null
    }

    private fun validate(request: SaveProfessionalRequest): ServiceError? = when {
        request.name.isBlank() || request.name.length > 200 -> ServiceError(ErrorCode.NAME_REQUIRED)
        request.registration.length > 40 -> ServiceError(ErrorCode.TEXT_TOO_LONG, 40)
        admin.specialties().none { it.id == request.specialtyId } -> ServiceError(ErrorCode.SPECIALTY_NOT_FOUND)
        admin.clinic(request.clinicId) == null -> ServiceError(ErrorCode.CLINIC_NOT_FOUND)
        else -> null
    }

    private fun now() = clock.instant().toKotlinInstant()

    companion object {
        /** Zones offered in the clinic form: Brazil's. Any IANA zone with a region is accepted. */
        val BRAZIL_TIME_ZONES = listOf(
            "America/Noronha", "America/Recife", "America/Fortaleza", "America/Belem", "America/Maceio",
            "America/Bahia", "America/Sao_Paulo", "America/Araguaina", "America/Cuiaba", "America/Campo_Grande",
            "America/Manaus", "America/Porto_Velho", "America/Boa_Vista", "America/Rio_Branco",
        )
    }
}
