package grupotp.recifesaudavel.app.ui.screen.scheduling.step

import grupotp.recifesaudavel.app.domain.scheduling.SchedulingOptions
import grupotp.recifesaudavel.app.ui.component.alert.infoAlert
import grupotp.recifesaudavel.app.ui.component.alert.warnAlert
import grupotp.recifesaudavel.app.ui.component.form.choiceChip
import grupotp.recifesaudavel.app.ui.component.form.fieldError
import grupotp.recifesaudavel.app.ui.component.form.formField
import grupotp.recifesaudavel.app.ui.component.layout.sectionTitle
import grupotp.recifesaudavel.app.ui.format.clinicZoneNote
import grupotp.recifesaudavel.app.ui.format.formatShortDate
import grupotp.recifesaudavel.app.ui.format.formatSpokenDate
import grupotp.recifesaudavel.app.ui.format.formatTime
import grupotp.recifesaudavel.app.ui.theme.Styles
import grupotp.recifesaudavel.common.scheduling.ClinicAvailability
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Props

internal external interface SchedulingStep2DateTimeProps : Props {
    var options: SchedulingOptions
    /** Open days per clinic, as the server just answered. */
    var availability: List<ClinicAvailability>
    var clinicId: Int?
    var date: LocalDate?
    var time: LocalTime?
    var fieldErrors: Map<String, String>
    var onClinic: (ClinicAvailability) -> Unit
    var onDate: (LocalDate) -> Unit
    var onTime: (LocalTime) -> Unit
}

/**
 * Step 2: clinic (when there is more than one), day and time, all from the server's open times.
 */
internal val SchedulingStep2DateTime = FC<SchedulingStep2DateTimeProps> { props ->
    val clinics = props.availability
    val clinic = props.options.clinic(props.clinicId)
    val openDays = clinics.firstOrNull { it.clinicId == props.clinicId }?.days.orEmpty()
    val fieldErrors = props.fieldErrors

    View {
        style = Styles.card
        sectionTitle("Data e Horário")
        infoAlert("Horários disponíveis são validados pelo servidor no momento da confirmação.")
        if (clinics.size > 1) {
            formField(caption = "Clínica", required = true) {
                View {
                    style = Styles.chipWrap
                    accessibilityRole = "radiogroup"
                    clinics.forEach { option ->
                        val place = props.options.clinic(option.clinicId)
                        choiceChip(
                            place?.let { "${it.name} · ${it.city}" } ?: option.clinicId.toString(),
                            selected = option.clinicId == props.clinicId,
                        ) { props.onClinic(option) }
                    }
                }
                fieldError(fieldErrors["clinic"])
            }
        }
        val note = clinic?.let { clinicZoneNote(it, props.date ?: openDays.firstOrNull()?.date ?: return@let null) }
        if (note != null) {
            infoAlert(note)
        }
        if (clinic == null && clinics.size > 1) {
            Text {
                style = Styles.fieldHint
                +"Escolha a clínica para ver as datas e os horários."
            }
        } else if (openDays.isEmpty()) {
            warnAlert(
                "Não há horários livres nos próximos 14 dias para esta escolha. " +
                    "Volte e escolha outro profissional ou especialidade.",
            )
        } else {
            formField(caption = "Data desejada", required = true) {
                View {
                    style = Styles.chipWrap
                    accessibilityRole = "radiogroup"
                    openDays.forEach { day ->
                        choiceChip(
                            text = formatShortDate(day.date),
                            selected = day.date == props.date,
                            accessibilityLabel = formatSpokenDate(day.date),
                        ) { props.onDate(day.date) }
                    }
                }
                fieldError(fieldErrors["date"])
            }
            val times = openDays.firstOrNull { it.date == props.date }?.times.orEmpty()
            formField(caption = "Horário desejado", required = true) {
                View {
                    style = Styles.chipWrap
                    accessibilityRole = "radiogroup"
                    times.forEach { option ->
                        choiceChip(formatTime(option), selected = option == props.time, style = Styles.timeChip) {
                            props.onTime(option)
                        }
                    }
                }
                if (times.isEmpty()) {
                    Text {
                        style = Styles.fieldHint
                        +"Escolha uma data para ver os horários."
                    }
                }
                fieldError(fieldErrors["time"])
            }
        }
    }
}
