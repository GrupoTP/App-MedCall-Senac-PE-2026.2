package grupotp.recifesaudavel.server.repository

import grupotp.recifesaudavel.common.pretriage.PreTriage
import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import grupotp.recifesaudavel.server.database.table.PreTriagesTable
import grupotp.recifesaudavel.server.database.entity.AppointmentEntity
import grupotp.recifesaudavel.server.database.entity.PreTriageEntity
import grupotp.recifesaudavel.server.database.entity.UserEntity
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

class PreTriageRepository(
    private val database: Database,
) {
    // coerceInputValues: an answer this version no longer knows reads as unanswered instead of
    // failing the whole list.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun insert(patientId: Int, form: PreTriageForm, createdAt: Instant): PreTriage = transaction(database) {
        PreTriageEntity.new {
            patient = UserEntity[patientId]
            this.form = json.encodeToString(form)
            this.createdAt = createdAt
        }.toPreTriage()
    }

    /** Most recent pre-triage of the patient, attached or not. */
    fun latestOf(patientId: Int): PreTriage? = transaction(database) {
        PreTriageEntity.find { PreTriagesTable.patient eq patientId }
            .orderBy(PreTriagesTable.createdAt to SortOrder.DESC, PreTriagesTable.id to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.toPreTriage()
    }

    /** The patient's own pre-triage. Null when it does not exist or belongs to someone else. */
    fun find(patientId: Int, id: Int): PreTriage? = transaction(database) {
        ownedBy(patientId, id)?.toPreTriage()
    }

    fun attach(patientId: Int, id: Int, appointmentId: Int): Boolean = transaction(database) {
        val preTriage = ownedBy(patientId, id) ?: return@transaction false
        preTriage.appointment = AppointmentEntity[appointmentId]
        true
    }

    private fun ownedBy(patientId: Int, id: Int): PreTriageEntity? =
        PreTriageEntity.find { (PreTriagesTable.id eq id) and (PreTriagesTable.patient eq patientId) }.singleOrNull()

    private fun PreTriageEntity.toPreTriage(): PreTriage = PreTriage(
        id = id.value,
        createdAt = createdAt,
        form = json.decodeFromString<PreTriageForm>(form),
        appointmentId = appointmentId?.value,
    )
}
