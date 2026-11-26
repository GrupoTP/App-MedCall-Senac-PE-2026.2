package grupotp.medcall.server.service

import java.time.Clock

/** Staff sessions with their own [SessionService], over the staff session table. */
class StaffSessionService(
    store: grupotp.medcall.server.repository.StaffSessionRepository,
    clock: Clock,
    settings: grupotp.medcall.server.security.SessionSettings,
) : SessionService(store, clock, settings)

