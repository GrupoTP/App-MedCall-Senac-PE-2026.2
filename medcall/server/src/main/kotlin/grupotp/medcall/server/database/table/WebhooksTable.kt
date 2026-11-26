package grupotp.medcall.server.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Where a clinic wants its events posted. The signing secret has to be read back to sign, so it is
 * kept as is; it only leaves the server once, when created or rotated.
 *
 * @property events wire names of [grupotp.medcall.common.integration.WebhookEvent], comma separated.
 * @property previousSecret still accepted until [previousSecretUntil] after a rotation.
 */
object WebhooksTable : IntIdTable("webhooks") {
    val clinic = reference("clinic_id", ClinicsTable).index()
    val url = varchar("url", 500)
    val description = varchar("description", 200).default("")
    val events = varchar("events", 300)
    val secret = varchar("secret", 100)
    val previousSecret = varchar("previous_secret", 100).nullable()
    val previousSecretUntil = timestamp("previous_secret_until").nullable()
    val secretRotatedAt = timestamp("secret_rotated_at").nullable()
    val active = bool("active")
    val disabledReason = varchar("disabled_reason", 200).default("")
    val failingSince = timestamp("failing_since").nullable()
    val createdAt = timestamp("created_at")
}

