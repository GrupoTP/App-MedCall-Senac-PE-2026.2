package grupotp.medcall.admin.pages.medcall

import react.FC
import react.Props

external interface ClinicsProps : Props {
    var clinicId: Int?
}


val MedcallClinicsPage = FC<ClinicsProps> { props ->
    val id = props.clinicId
    if (id != null) {
        ClinicDetail { clinicId = id }
    } else {
        ClinicList()
    }
}

