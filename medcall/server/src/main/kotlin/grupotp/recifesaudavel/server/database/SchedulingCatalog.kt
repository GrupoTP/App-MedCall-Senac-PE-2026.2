package grupotp.recifesaudavel.server.database

/**
 * Clinics, specialties and professionals from the prototype, inserted when the tables are empty.
 * The database numbers them; code that needs one finds it by name.
 *
 * Most professionals attend in Recife. Psychiatry is also offered by teleconsultation from Manaus,
 * one hour behind, so the catalog exercises a specialty spread over two time zones.
 */
object SchedulingCatalog {
    data class CatalogClinic(val name: String, val city: String, val timeZoneId: String)

    data class CatalogProfessional(val name: String, val clinic: CatalogClinic)

    val boaVista = CatalogClinic("Clínica Boa Vista", "Recife", "America/Recife")
    val manaus = CatalogClinic("Teleconsulta Manaus", "Manaus", "America/Manaus")
    val clinics = listOf(boaVista, manaus)

    val professionalsBySpecialty: Map<String, List<CatalogProfessional>> = linkedMapOf(
        "Clínica Geral" to inRecife("Dr. Roberto Silva", "Dra. Maria Santos", "Dr. Paulo Costa"),
        "Cardiologia" to inRecife("Dr. Antônio Ferreira", "Dra. Lúcia Oliveira"),
        "Ortopedia" to inRecife("Dr. Fernando Gomes", "Dra. Patrícia Alves"),
        "Neurologia" to inRecife("Dra. Cláudia Martins", "Dr. José Pereira"),
        "Ginecologia" to inRecife("Dra. Sandra Lima", "Dra. Fernanda Ramos"),
        "Pediatria" to inRecife("Dr. Marcos Souza", "Dra. Juliana Castro"),
        "Dermatologia" to inRecife("Dra. Renata Barbosa", "Dr. Eduardo Nascimento"),
        "Oftalmologia" to inRecife("Dr. Rogério Araújo", "Dra. Cristina Cunha"),
        "Urologia" to inRecife("Dr. Henrique Melo", "Dr. Leonardo Dias"),
        "Psiquiatria" to listOf(
            CatalogProfessional("Dra. Beatriz Rocha", boaVista),
            CatalogProfessional("Dr. Alexandre Nunes", manaus),
        ),
        "Endocrinologia" to inRecife("Dra. Isabela Cardoso", "Dr. Thiago Vieira"),
    )

    private fun inRecife(vararg names: String): List<CatalogProfessional> = names.map { CatalogProfessional(it, boaVista) }
}
