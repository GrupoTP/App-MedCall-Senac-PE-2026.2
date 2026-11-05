package grupotp.medcall.app.ui.component.text

import grupotp.medcall.app.ui.format.label
import grupotp.medcall.app.ui.theme.Styles
import grupotp.medcall.common.scheduling.AppointmentStatus
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.ChildrenBuilder

internal fun ChildrenBuilder.statusPill(status: AppointmentStatus) {
    View {
        style = arrayOf(
            Styles.statusPill,
            when (status) {
                AppointmentStatus.Pending -> Styles.statusPending
                AppointmentStatus.Confirmed -> Styles.statusConfirmed
                AppointmentStatus.Cancelled -> Styles.statusCancelled
            },
        )
        Text {
            style = when (status) {
                AppointmentStatus.Pending -> Styles.statusPendingText
                AppointmentStatus.Confirmed -> Styles.statusConfirmedText
                AppointmentStatus.Cancelled -> Styles.statusCancelledText
            }
            +status.label()
        }
    }
}
