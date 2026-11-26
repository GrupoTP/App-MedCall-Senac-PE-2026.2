package grupotp.medcall.server.security

/** Tokens of the staff portals: same key, another audience, so a patient token never opens them. */
class StaffTokens(settings: JwtSettings) : SessionTokens(settings, STAFF_AUDIENCE) {
    companion object {
        const val STAFF_AUDIENCE = "medcall-staff"
    }
}

