package grupotp.medcall.common.staff

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who a staff account is. Staff accounts are separate from patient accounts: a patient token is
 * refused on the staff routes and the other way around.
 */
@Serializable
enum class StaffRole {
    /** MedCall's own team. Sees the whole platform, only as aggregates. */
    @SerialName("medcall_admin")
    MedcallAdmin,

    /** Manages one clinic: users, dashboard, integrations and the agenda. */
    @SerialName("clinic_admin")
    ClinicAdmin,

    /** Works one clinic's agenda. */
    @SerialName("receptionist")
    Receptionist,
    ;

    val isClinicRole: Boolean get() = this != MedcallAdmin
}

