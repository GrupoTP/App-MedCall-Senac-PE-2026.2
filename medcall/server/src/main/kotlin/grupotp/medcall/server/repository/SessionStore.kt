package grupotp.medcall.server.repository

import grupotp.medcall.server.database.entity.SessionEntity
import grupotp.medcall.server.domain.StoredSession
import kotlin.time.Instant

/** Single sessions go through [SessionEntity]; deleting many at once stays one SQL statement. */
/** Where [grupotp.medcall.server.service.SessionService] keeps sessions: patients' or staff's. */
interface SessionStore {
    fun insert(userId: Int, createdAt: Instant): Int
    fun find(id: Int): StoredSession?
    fun touch(id: Int, at: Instant)
    fun delete(id: Int)
    fun deleteAllOf(userId: Int, keepId: Int? = null)
    fun deleteUnusedSince(cutoff: Instant)
}

