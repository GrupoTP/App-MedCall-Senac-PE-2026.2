package grupotp.medcall.server.repository

sealed interface ChangeOutcome {
    data object Applied : ChangeOutcome
    /** Someone changed it since it was read. */
    data object Changed : ChangeOutcome
    /** The new slot of a reschedule was taken. */
    data object SlotTaken : ChangeOutcome
}

