package grupotp.recifesaudavel.server.database

import java.util.UUID

/**
 * Specialties and professionals from the prototype, inserted when the tables are empty.
 * Ids derive from the names, so every database gets the same ones.
 */
object SchedulingCatalog {
    val professionalsBySpecialty: Map<String, List<String>> = linkedMapOf(
        "Clínica Geral" to listOf("Dr. Roberto Silva", "Dra. Maria Santos", "Dr. Paulo Costa"),
        "Cardiologia" to listOf("Dr. Antônio Ferreira", "Dra. Lúcia Oliveira"),
        "Ortopedia" to listOf("Dr. Fernando Gomes", "Dra. Patrícia Alves"),
        "Neurologia" to listOf("Dra. Cláudia Martins", "Dr. José Pereira"),
        "Ginecologia" to listOf("Dra. Sandra Lima", "Dra. Fernanda Ramos"),
        "Pediatria" to listOf("Dr. Marcos Souza", "Dra. Juliana Castro"),
        "Dermatologia" to listOf("Dra. Renata Barbosa", "Dr. Eduardo Nascimento"),
        "Oftalmologia" to listOf("Dr. Rogério Araújo", "Dra. Cristina Cunha"),
        "Urologia" to listOf("Dr. Henrique Melo", "Dr. Leonardo Dias"),
        "Psiquiatria" to listOf("Dra. Beatriz Rocha", "Dr. Alexandre Nunes"),
        "Endocrinologia" to listOf("Dra. Isabela Cardoso", "Dr. Thiago Vieira"),
    )

    fun specialtyId(name: String): String = stableId("specialty:$name")

    fun professionalId(specialty: String, name: String): String = stableId("professional:$specialty:$name")

    private fun stableId(key: String): String = UUID.nameUUIDFromBytes(key.toByteArray()).toString()
}
