package grupotp.medcall.admin.ui

import kotlinx.coroutines.awaitCancellation
import react.FC
import react.Props
import react.dom.html.ReactHTML.canvas
import react.dom.html.ReactHTML.div
import react.useEffect
import react.useRef
import web.cssom.ClassName
import web.html.HTMLCanvasElement

/** Creates a Chart.js chart on [canvas]. */
private fun newChart(canvas: HTMLCanvasElement, config: dynamic): dynamic {
    Charts.ensureDefaults()
    val Chart = chartClass
    return js("new Chart(canvas, config)")
}


external interface ChartProps : Props {
    /** A Chart.js configuration (type, data, options). */
    var config: dynamic

    /** Height in pixels; the width follows the card. */
    var height: Int

    /** Accessible description of what the chart shows. */
    var description: String
}


/**
 * A Chart.js chart that is rebuilt when its data changes. The JSON of the config is the
 * dependency, so a re-render with the same numbers keeps the chart (no flicker).
 */
val ChartView = FC<ChartProps> { props ->
    val canvasRef = useRef<HTMLCanvasElement>(null)
    val signature: String = JSON.stringify(props.config)
    useEffect(signature) {
        val canvas = canvasRef.current ?: return@useEffect
        val chart = newChart(canvas, props.config)
        try {
            awaitCancellation()
        } finally {
            chart.destroy()
        }
    }
    div {
        className = ClassName("chart-box")
        style = js("({})").unsafeCast<react.CSSProperties>().also { it.asDynamic().height = "${props.height}px" }
        canvas {
            ref = canvasRef
            asDynamic().role = "img"
            ariaLabel = props.description
        }
    }
}

