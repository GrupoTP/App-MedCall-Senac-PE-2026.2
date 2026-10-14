package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.PreTriage
import grupotp.recifesaudavel.common.PreTriageForm
import grupotp.recifesaudavel.server.database.PreTriagesTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlinx.serialization.json.Json

class PreTriageRepository(
    private val database: Database,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun insert(id: String, patientId: String, form: PreTriageForm, createdAt: Long): PreTriage = transaction(database) {
        PreTriagesTable.insert {
            it[PreTriagesTable.id] = id
            it[PreTriagesTable.patientId] = patientId
            it[PreTriagesTable.form] = json.encodeToString(form)
            it[PreTriagesTable.createdAt] = createdAt
            it[appointmentId] = null
        }
        PreTriage(id = id, createdAt = createdAt, form = form)
    }

    /** Most recent pre-triage of the patient, attached or not. */
    fun latestOf(patientId: String): PreTriage? = transaction(database) {
        PreTriagesTable.selectAll()
            .where { PreTriagesTable.patientId eq patientId }
            .orderBy(PreTriagesTable.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.toPreTriage()
    }

    /** The patient's own pre-triage. Null when it does not exist or belongs to someone else. */
    fun find(patientId: String, id: String): PreTriage? = transaction(database) {
        PreTriagesTable.selectAll()
            .where { (PreTriagesTable.id eq id) and (PreTriagesTable.patientId eq patientId) }
            .singleOrNull()
            ?.toPreTriage()
    }

    fun attach(patientId: String, id: String, appointmentId: String): Boolean = transaction(database) {
        PreTriagesTable.update({ (PreTriagesTable.id eq id) and (PreTriagesTable.patientId eq patientId) }) {
            it[PreTriagesTable.appointmentId] = appointmentId
        } > 0
    }

    private fun ResultRow.toPreTriage(): PreTriage = PreTriage(
        id = this[PreTriagesTable.id],
        createdAt = this[PreTriagesTable.createdAt],
        form = json.decodeFromString<PreTriageForm>(this[PreTriagesTable.form]),
        appointmentId = this[PreTriagesTable.appointmentId],
    )
}
