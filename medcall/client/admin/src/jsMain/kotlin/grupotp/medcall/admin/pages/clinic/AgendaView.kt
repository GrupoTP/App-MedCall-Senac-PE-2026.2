package grupotp.medcall.admin.pages.clinic

import react.dom.html.ReactHTML.label

/** Quick views of the agenda. */
internal enum class AgendaView(val label: String, val icon: String) {
    Today("Hoje", "bi-calendar-day"),
    Week("Próximos 7 dias", "bi-calendar-week"),
    Pending("Aguardando resposta", "bi-hourglass-split"),
    Attendance("Sem registro de comparecimento", "bi-clipboard-check"),
}

