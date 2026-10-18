package grupotp.recifesaudavel.app.ui.screen.pretriage.step

import grupotp.recifesaudavel.common.pretriage.PreTriageForm
import react.Props

/**
 * What every pre-triage step receives. The screen keeps the form; a step only shows it and reports edits.
 */
internal external interface PreTriageStepProps : Props {
    var form: PreTriageForm
    var fieldErrors: Map<String, String>

    /** Applies the change to the form and clears the error of the field named by the key. */
    var onChange: (key: String, change: (PreTriageForm) -> PreTriageForm) -> Unit
}
