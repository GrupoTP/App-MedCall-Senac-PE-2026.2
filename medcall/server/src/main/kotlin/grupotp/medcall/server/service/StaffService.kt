package grupotp.medcall.server.service

import grupotp.medcall.common.api.ErrorCode
import grupotp.medcall.common.security.SecretString
import grupotp.medcall.common.staff.ChangeStaffPasswordRequest
import grupotp.medcall.common.staff.CreateStaffUserRequest
import grupotp.medcall.common.staff.StaffAccount
import grupotp.medcall.common.staff.StaffRole
import grupotp.medcall.common.staff.StaffSignInRequest
import grupotp.medcall.common.staff.StaffUser
import grupotp.medcall.common.staff.StaffUserWithPassword
import grupotp.medcall.common.staff.UpdateStaffUserRequest
import grupotp.medcall.server.domain.ServiceError
import grupotp.medcall.server.repository.AuditRepository
import grupotp.medcall.server.repository.StaffRepository
import grupotp.medcall.server.repository.StaffUpdateOutcome
import grupotp.medcall.server.repository.StoredStaff
import grupotp.medcall.server.security.PasswordHasher
import grupotp.medcall.server.security.StaffPrincipal
import grupotp.medcall.server.security.StaffTokens
import java.security.SecureRandom
import java.time.Clock
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

sealed interface StaffResult<out T> {
    data class Done<T>(val value: T) : StaffResult<T>
    data class Rejected(val error: ServiceError) : StaffResult<Nothing>
    data object NotFound : StaffResult<Nothing>
}

/** Staff sessions with their own [SessionService], over the staff session table. */
class StaffSessionService(
    store: grupotp.medcall.server.repository.StaffSessionRepository,
    clock: Clock,
    settings: grupotp.medcall.server.security.SessionSettings,
) : SessionService(store, clock, settings)

/**
 * Accounts of the clinic portal and the MedCall panel: sign-in, temporary passwords and the
 * management of a clinic's team. Every check is here, whatever the screen hides.
 */
class StaffService(
    private val staff: StaffRepository,
    private val passwords: PasswordHasher,
    private val sessions: StaffSessionService,
    private val tokens: StaffTokens,
    private val audit: AuditRepository,
    private val clock: Clock,
) {
    /** Null for an unknown e-mail, a wrong password or a switched-off account; which one is not told. */
    fun signIn(request: StaffSignInRequest): StaffAccount? {
        val stored = staff.findByEmail(request.email.trim().lowercase()) ?: return null
        if (!stored.user.active || !passwords.verify(request.password, stored.passwordHash)) {
            return null
        }
        if (passwords.needsRehash(stored.passwordHash)) {
            staff.setPassword(stored.user.id, passwords.hash(request.password), stored.user.mustChangePassword)
        }
        staff.recordLogin(stored.user.id, now())
        val session = sessions.open(stored.user.id)
        return stored.toAccount().copy(sessionToken = tokens.sign(stored.user.id, session))
    }

    /** The principal of a staff token, or null when the account is gone or switched off. */
    fun principalOf(staffId: Int, sessionId: Int): StaffPrincipal? {
        val stored = staff.findById(staffId)?.takeIf { it.user.active } ?: return null
        return StaffPrincipal(
            staffId = staffId,
            sessionId = sessionId,
            role = stored.user.role,
            clinicId = stored.user.clinicId,
            fullName = stored.user.fullName,
            mustChangePassword = stored.user.mustChangePassword,
        )
    }

    fun account(staffId: Int): StaffAccount? = staff.findById(staffId)?.toAccount()

    fun signOut(sessionId: Int) = sessions.revoke(sessionId)

    /** Changes the password and ends the account's other sessions. */
    fun changePassword(principal: StaffPrincipal, request: ChangeStaffPasswordRequest): StaffResult<StaffAccount> {
        val stored = staff.findById(principal.staffId) ?: return StaffResult.NotFound
        if (!passwords.verify(request.currentPassword, stored.passwordHash)) {
            return StaffResult.Rejected(ServiceError(ErrorCode.CURRENT_PASSWORD_WRONG))
        }
        if (request.newPassword.value.length < MIN_PASSWORD) {
            return StaffResult.Rejected(ServiceError(ErrorCode.PASSWORD_TOO_SHORT, MIN_PASSWORD))
        }
        if (request.newPassword.value == request.currentPassword.value) {
            return StaffResult.Rejected(ServiceError(ErrorCode.PASSWORD_UNCHANGED))
        }
        staff.setPassword(stored.user.id, passwords.hash(request.newPassword), mustChangePassword = false)
        sessions.revokeAll(stored.user.id, keepId = principal.sessionId)
        return StaffResult.Done(staff.findById(stored.user.id)!!.toAccount())
    }

    fun clinicTeam(clinicId: Int): List<StaffUser> = staff.listClinicStaff(clinicId)

    fun allClinicStaff(): List<StaffUser> = staff.listClinicStaff(null)

    /**
     * Creates a clinic account with a temporary password, shown once. Only clinic roles: MedCall's
     * own team is created from the environment ([bootstrap]).
     */
    fun createClinicUser(actor: StaffPrincipal, clinicId: Int, request: CreateStaffUserRequest): StaffResult<StaffUserWithPassword> {
        val name = request.fullName.trim()
        val email = request.email.trim().lowercase()
        val problem = when {
            name.isEmpty() -> ErrorCode.NAME_REQUIRED
            !email.contains("@") || email.length > 200 -> ErrorCode.EMAIL_INVALID
            !request.role.isClinicRole -> ErrorCode.ROLE_INVALID
            else -> null
        }
        if (problem != null) {
            return StaffResult.Rejected(ServiceError(problem))
        }
        val password = temporaryPassword()
        val user = staff.insert(name, email, request.role, clinicId, passwords.hash(password), mustChangePassword = true, now())
            ?: return StaffResult.Rejected(ServiceError(ErrorCode.EMAIL_TAKEN))
        audit.record(now(), "staff.created", "${user.fullName} (${roleLabel(user.role)})", clinicId, actor.staffId)
        return StaffResult.Done(StaffUserWithPassword(user, password))
    }

    fun updateClinicUser(actor: StaffPrincipal, clinicId: Int, id: Int, request: UpdateStaffUserRequest): StaffResult<StaffUser> {
        if (id == actor.staffId && (request.active == false || (request.role != null && request.role != actor.role))) {
            return StaffResult.Rejected(ServiceError(ErrorCode.CANNOT_CHANGE_SELF))
        }
        if (request.role?.isClinicRole == false) {
            return StaffResult.Rejected(ServiceError(ErrorCode.ROLE_INVALID))
        }
        val name = request.fullName?.trim()
        if (name != null && name.isEmpty()) {
            return StaffResult.Rejected(ServiceError(ErrorCode.NAME_REQUIRED))
        }
        return when (val outcome = staff.updateClinicUser(clinicId, id, name, request.role, request.active)) {
            StaffUpdateOutcome.NotFound -> StaffResult.NotFound
            StaffUpdateOutcome.LastAdmin -> StaffResult.Rejected(ServiceError(ErrorCode.LAST_ADMIN))
            is StaffUpdateOutcome.Updated -> {
                if (!outcome.user.active) {
                    sessions.revokeAll(id)
                }
                audit.record(now(), "staff.updated", describe(outcome.user), clinicId, actor.staffId)
                StaffResult.Done(outcome.user)
            }
        }
    }

    /**
     * New temporary password for a clinic account, ending its sessions. [clinicId] null lets MedCall's
     * team reset any clinic account.
     */
    fun resetPassword(actor: StaffPrincipal, clinicId: Int?, id: Int): StaffResult<StaffUserWithPassword> {
        val stored = staff.findById(id)?.takeIf { it.user.clinicId != null && (clinicId == null || it.user.clinicId == clinicId) }
            ?: return StaffResult.NotFound
        if (id == actor.staffId) {
            return StaffResult.Rejected(ServiceError(ErrorCode.CANNOT_CHANGE_SELF))
        }
        val password = temporaryPassword()
        staff.setPassword(id, passwords.hash(password), mustChangePassword = true)
        sessions.revokeAll(id)
        audit.record(now(), "staff.password_reset", stored.user.fullName, stored.user.clinicId, actor.staffId)
        return StaffResult.Done(StaffUserWithPassword(staff.findById(id)!!.user, password))
    }

    /** MedCall's team switching a clinic account off (incident) or back on. */
    fun setActiveByMedcall(actor: StaffPrincipal, id: Int, active: Boolean): StaffResult<StaffUser> {
        val stored = staff.findById(id)?.takeIf { it.user.clinicId != null } ?: return StaffResult.NotFound
        staff.setActive(id, active)
        if (!active) {
            sessions.revokeAll(id)
        }
        audit.record(now(), if (active) "staff.reactivated" else "staff.deactivated", stored.user.fullName, stored.user.clinicId, actor.staffId)
        return StaffResult.Done(staff.findById(id)!!.user)
    }

    /**
     * Creates the first MedCall administrator from the environment when there is no staff account
     * yet, so no password lives in the code. Does nothing when either value is blank.
     */
    fun bootstrap(email: String, password: SecretString) {
        if (email.isBlank() || password.isBlank() || !staff.isEmpty()) {
            return
        }
        staff.insert("Equipe MedCall", email.trim().lowercase(), StaffRole.MedcallAdmin, null, passwords.hash(password), false, now())
    }

    /** Creates an account with a known password, for the demo data only. */
    fun createDemoAccount(fullName: String, email: String, role: StaffRole, clinicId: Int?, passwordHash: String) {
        if (staff.findByEmail(email) == null) {
            staff.insert(fullName, email, role, clinicId, passwordHash, mustChangePassword = false, now())
        }
    }

    private fun describe(user: StaffUser): String =
        "${user.fullName} (${roleLabel(user.role)}, ${if (user.active) "ativo" else "inativo"})"

    private fun roleLabel(role: StaffRole): String = when (role) {
        StaffRole.MedcallAdmin -> "equipe MedCall"
        StaffRole.ClinicAdmin -> "administrador"
        StaffRole.Receptionist -> "recepcionista"
    }

    private fun StoredStaff.toAccount() = StaffAccount(
        id = user.id,
        fullName = user.fullName,
        email = user.email,
        role = user.role,
        clinicId = user.clinicId,
        clinicName = clinicName,
        mustChangePassword = user.mustChangePassword,
    )

    private fun now(): Instant = clock.instant().toKotlinInstant()

    companion object {
        const val MIN_PASSWORD = 8
        private const val TEMPORARY_LENGTH = 12

        /** No 0/O, 1/l/I: the password is read aloud or copied by hand. */
        private const val ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        fun temporaryPassword(): SecretString =
            SecretString(String(CharArray(TEMPORARY_LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] }))
    }
}
