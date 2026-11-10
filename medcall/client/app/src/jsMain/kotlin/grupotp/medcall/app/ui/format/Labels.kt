package grupotp.medcall.app.ui.format

import grupotp.medcall.common.pretriage.SymptomsStart
import grupotp.medcall.common.scheduling.Appointment
import grupotp.medcall.common.scheduling.AppointmentStatus

// Text shown for the shared enums. The enums only carry codes; the words live here, with an
// exhaustive `when`, so a new value or another language cannot be missed.

internal fun AppointmentStatus.label(): String = when (this) {
    AppointmentStatus.Pending -> "Aguardando"
    AppointmentStatus.Confirmed -> "Confirmada"
    AppointmentStatus.Declined -> "Recusada"
    AppointmentStatus.Cancelled -> "Cancelada"
    AppointmentStatus.Attended -> "Realizada"
    AppointmentStatus.NoShow -> "Falta"
}

/** Like [AppointmentStatus.label], but says when the clinic moved a confirmed appointment. */
internal fun Appointment.statusLabel(): String =
    if (rescheduled && status == AppointmentStatus.Confirmed) "Remarcada" else status.label()

/** Answers of "Quando começaram os sintomas?". */
internal fun SymptomsStart.label(): String = when (this) {
    SymptomsStart.Today -> "Hoje"
    SymptomsStart.TwoToThreeDays -> "Há 2 a 3 dias"
    SymptomsStart.OneWeek -> "Há uma semana"
    SymptomsStart.TwoWeeks -> "Há 2 semanas"
    SymptomsStart.OneMonth -> "Há 1 mês"
    SymptomsStart.OverOneMonth -> "Há mais de 1 mês"
    SymptomsStart.OverSixMonths -> "Há mais de 6 meses"
    SymptomsStart.Chronic -> "Crônico (mais de 1 ano)"
}
