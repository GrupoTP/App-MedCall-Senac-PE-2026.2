package grupotp.recifesaudavel.app.ui.component.layout

import grupotp.recifesaudavel.app.ui.theme.Styles
import ojaynico.kotlin.react.native.component.Text
import ojaynico.kotlin.react.native.component.View
import react.FC
import react.Key
import react.Props

internal external interface StepIndicatorProps : Props {
    var current: Int
    var labels: Array<String>
}

/**
 * Port of the prototype step indicator: counter, remaining steps, current label and segmented bars.
 * The whole state is also announced as one sentence for screen readers.
 */
internal val StepIndicator = FC<StepIndicatorProps> { props ->
    val total = props.labels.size
    val current = props.current
    val remaining = total - current - 1
    val last = remaining == 0
    val label = props.labels[current]
    val done = if (current == 1) "1 etapa concluída" else "$current etapas concluídas"
    val left = if (remaining == 1) "Falta 1 etapa" else "Faltam $remaining etapas"
    View {
        style = Styles.steps
        View {
            style = Styles.stepsInner
            accessible = true
            accessibilityRole = "progressbar"
            accessibilityLabel = if (last) {
                "Etapa ${current + 1} de $total: $label. Última etapa. $done."
            } else {
                "Etapa ${current + 1} de $total: $label. $done. $left."
            }
            View {
                style = Styles.stepsTop
                Text {
                    style = Styles.stepsCounter
                    +"Etapa ${current + 1} de $total"
                }
                View {
                    style = arrayOf(Styles.stepsRemaining, if (last) Styles.stepsRemainingLast else null)
                    Text {
                        style = if (last) Styles.stepsRemainingTextLast else Styles.stepsRemainingText
                        +(if (last) "✓ Última etapa" else left)
                    }
                }
            }
            Text {
                style = Styles.stepsLabel
                +label
            }
            View {
                style = Styles.stepsBars
                for (index in 0 until total) {
                    View {
                        key = Key(index.toString())
                        style = Styles.stepsColumn
                        Text {
                            style = arrayOf(
                                Styles.stepsNumber,
                                when {
                                    index < current -> Styles.stepsNumberDone
                                    index == current -> null
                                    else -> Styles.stepsNumberPending
                                },
                            )
                            +(index + 1).toString()
                        }
                        View {
                            style = arrayOf(
                                Styles.stepsBar,
                                when {
                                    index < current -> Styles.stepsBarDone
                                    index == current -> Styles.stepsBarActive
                                    else -> null
                                },
                            )
                        }
                    }
                }
            }
            Text {
                style = Styles.stepsNote
                +(if (current == 0) "Nenhuma etapa concluída ainda" else done)
                if (last) {
                    +" · Complete esta para finalizar"
                }
            }
        }
    }
}
