package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * One-time password recovery codes. Only the SHA-256 of the code is stored.
 * The plain code is sent by e-mail and is never written here.
 */
object PasswordResetsTable : IntIdTable("password_resets") {
    val user = reference("user_id", UsersTable).index()
    val codeHash = varchar("code_hash", 64)
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
}
