package grupotp.medcall.server.repository

import grupotp.medcall.server.database.entity.PasswordResetEntity
import grupotp.medcall.server.database.entity.UserEntity
import grupotp.medcall.server.database.table.PasswordResetsTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.security.MessageDigest
import kotlin.time.Instant

/** Stores only the hash of a recovery code. A new request drops the unused ones of that account. */
class PasswordResetRepository(
    private val database: Database,
) {
    fun replace(userId: Int, codeHash: String, expiresAt: Instant) {
        transaction(database) {
            PasswordResetsTable.deleteWhere {
                (PasswordResetsTable.user eq userId) and PasswordResetsTable.usedAt.isNull()
            }
            PasswordResetEntity.new {
                user = UserEntity[userId]
                this.codeHash = codeHash
                this.expiresAt = expiresAt
            }
        }
    }

    /** Marks one unused, unexpired code as used. Returns false when none matches. */
    fun consume(userId: Int, codeHash: String, now: Instant): Boolean = transaction(database) {
        val row = PasswordResetEntity.find {
            (PasswordResetsTable.user eq userId) and
                PasswordResetsTable.usedAt.isNull() and
                (PasswordResetsTable.expiresAt greater now)
        }.firstOrNull { sameHash(it.codeHash, codeHash) } ?: return@transaction false
        row.usedAt = now
        true
    }

    private fun sameHash(stored: String, offered: String): Boolean =
        MessageDigest.isEqual(stored.toByteArray(Charsets.UTF_8), offered.toByteArray(Charsets.UTF_8))
}
