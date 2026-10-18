package grupotp.recifesaudavel.server

import grupotp.recifesaudavel.common.account.Phone
import grupotp.recifesaudavel.common.auth.AuthenticatedAccount
import grupotp.recifesaudavel.common.auth.RegisterAccountRequest
import grupotp.recifesaudavel.common.scheduling.Appointment
import grupotp.recifesaudavel.common.scheduling.AppointmentStatus
import grupotp.recifesaudavel.common.scheduling.Clinic
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability
import grupotp.recifesaudavel.common.scheduling.CreateAppointmentRequest
import grupotp.recifesaudavel.common.scheduling.Professional
import grupotp.recifesaudavel.common.scheduling.Specialty
import grupotp.recifesaudavel.common.security.SecretString
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchedulingRouteTest {
    @Test
    fun bookAnOpenSlotAndListItBack() = testApplication {
        // Arrange
        environment {
            config = ApplicationConfig("application.yaml")
        }
        val token = register(client).sessionToken.value
        val specialties = Json.decodeFromString<List<Specialty>>(client.get("/api/v1/specialties") { bearerAuth(token) }.bodyAsText())
        val specialty = specialties.first { it.name == "Dermatologia" }
        val professionals = Json.decodeFromString<List<Professional>>(
            client.get("/api/v1/professionals?specialtyId=${specialty.id}") { bearerAuth(token) }.bodyAsText(),
        )
        val clinics = Json.decodeFromString<List<Clinic>>(client.get("/api/v1/clinics") { bearerAuth(token) }.bodyAsText())
        // The file database outlives the test, so pick whatever slot is still open.
        val days = Json.decodeFromString<List<ClinicAvailability>>(
            client.get("/api/v1/availability?specialtyId=${specialty.id}") { bearerAuth(token) }.bodyAsText(),
        )
        val day = days.single().days.first()

        // Act
        val created = client.post("/api/v1/appointments") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    CreateAppointmentRequest(
                        specialtyId = specialty.id,
                        professionalId = null,
                        date = day.date,
                        time = day.times.first(),
                        reason = "Mancha na pele",
                        notes = "",
                    ),
                ),
            )
        }
        val listed = client.get("/api/v1/appointments") { bearerAuth(token) }

        // Assert
        assertEquals(11, specialties.size)
        assertEquals(2, professionals.size)
        assertEquals(setOf("Recife", "Manaus"), clinics.map { it.city }.toSet())
        assertTrue(professionals.all { professional -> clinics.any { it.id == professional.clinicId } })
        assertEquals(HttpStatusCode.Created, created.status)
        val appointment = Json.decodeFromString<Appointment>(created.bodyAsText())
        assertEquals("Dermatologia", appointment.specialtyName)
        assertEquals(day.date, appointment.date)
        assertEquals(listOf(appointment), Json.decodeFromString<List<Appointment>>(listed.bodyAsText()))
    }

    @Test
    fun cancelFreesTheSlotAndOnlyTheOwnerCanCancel() = testApplication {
        // Arrange
        environment {
            config = ApplicationConfig("application.yaml")
        }
        val owner = register(client)
        val stranger = register(client)
        val specialty = Json.decodeFromString<List<Specialty>>(client.get("/api/v1/specialties") { bearerAuth(owner.sessionToken.value) }.bodyAsText())
            .first { it.name == "Cardiologia" }
        val day = Json.decodeFromString<List<ClinicAvailability>>(
            client.get("/api/v1/availability?specialtyId=${specialty.id}") { bearerAuth(owner.sessionToken.value) }.bodyAsText(),
        ).single().days.first()
        val created = client.post("/api/v1/appointments") {
            bearerAuth(owner.sessionToken.value)
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    CreateAppointmentRequest(specialty.id, null, date = day.date, time = day.times.last(), reason = "", notes = ""),
                ),
            )
        }
        val appointment = Json.decodeFromString<Appointment>(created.bodyAsText())

        // Act
        val byStranger = client.post("/api/v1/appointments/${appointment.id}/cancel") { bearerAuth(stranger.sessionToken.value) }
        val byOwner = client.post("/api/v1/appointments/${appointment.id}/cancel") { bearerAuth(owner.sessionToken.value) }

        // Assert
        assertEquals(HttpStatusCode.NotFound, byStranger.status)
        assertEquals(HttpStatusCode.OK, byOwner.status)
        assertEquals(AppointmentStatus.Cancelled, Json.decodeFromString<Appointment>(byOwner.bodyAsText()).status)
    }

    @Test
    fun patientRoutesRequireAValidSession() = testApplication {
        // Arrange
        environment {
            config = ApplicationConfig("application.yaml")
        }

        // Act
        val anonymous = client.get("/api/v1/appointments")
        val forged = client.get("/api/v1/appointments") { bearerAuth("token-inventado") }
        val availability = client.get("/api/v1/availability?specialtyId=x")
        val specialties = client.get("/api/v1/specialties")

        // Assert
        assertEquals(HttpStatusCode.Unauthorized, anonymous.status)
        assertEquals(HttpStatusCode.Unauthorized, forged.status)
        assertEquals(HttpStatusCode.Unauthorized, availability.status)
        assertEquals(HttpStatusCode.Unauthorized, specialties.status)
        assertTrue(anonymous.bodyAsText().contains("sessão"))
    }

    private suspend fun register(client: HttpClient): AuthenticatedAccount {
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterAccountRequest(
                        fullName = "Paciente de Teste",
                        socialName = "",
                        email = "agenda.${System.nanoTime()}@email.com",
                        password = SecretString("segredo"),
                        primaryPhone = Phone.parse("(81) 99999-9999"),
                        primaryPhoneWhatsapp = false,
                        secondaryPhone = Phone.parse(""),
                        secondaryPhoneWhatsapp = false,
                        emergencyContactName = "",
                        emergencyContactPhone = Phone.parse(""),
                        acceptedPrivacyPolicy = true,
                        acceptedTreatment = true,
                        wantsNotifications = false,
                    ),
                ),
            )
        }
        return Json.decodeFromString(response.bodyAsText())
    }
}
