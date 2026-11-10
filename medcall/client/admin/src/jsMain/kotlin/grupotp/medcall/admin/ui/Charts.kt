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

@JsModule("chart.js/auto")
private external val chartModule: dynamic

/** The Chart class: the CommonJS build exports it as the module itself, the ES one as `default`. */
private val chartClass: dynamic get() = chartModule.default ?: chartModule

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

/** Builders of Chart.js configs with the MedCall look. */
object Charts {
    init {
        val defaults = chartClass.defaults
        defaults.font.family = "Nunito, system-ui, sans-serif"
        defaults.color = Brand.MUTED
        defaults.plugins.legend.labels.usePointStyle = true
        defaults.maintainAspectRatio = false
    }

    fun ensureDefaults() = Unit

    fun dataset(label: String, data: List<Number?>, color: String, type: String? = null, extra: dynamic = null): dynamic {
        val set: dynamic = js("({})")
        set.label = label
        set.data = data.map { it?.toDouble() }.toTypedArray()
        set.backgroundColor = color
        set.borderColor = color
        set.borderWidth = 2
        set.borderRadius = 4
        set.pointRadius = 2
        set.tension = 0.3
        if (type != null) set.type = type
        if (extra != null) js("Object.assign(set, extra)")
        return set
    }

    fun config(
        type: String,
        labels: List<String>,
        datasets: List<dynamic>,
        stacked: Boolean = false,
        percentAxis: Boolean = false,
        horizontal: Boolean = false,
        legend: Boolean = true,
        yTitle: String? = null,
    ): Any {
        val config: dynamic = js("({ data: {}, options: { plugins: { legend: {}, tooltip: {} }, scales: { x: { grid: {} }, y: { grid: {}, title: {} } } } })")
        config.type = type
        config.data.labels = labels.toTypedArray()
        config.data.datasets = datasets.toTypedArray()
        config.options.responsive = true
        config.options.maintainAspectRatio = false
        config.options.interaction = js("({ mode: 'index', intersect: false })")
        config.options.plugins.legend.display = legend
        config.options.plugins.legend.position = "bottom"
        if (horizontal) config.options.indexAxis = "y"
        val valueAxis = if (horizontal) config.options.scales.x else config.options.scales.y
        config.options.scales.x.stacked = stacked
        config.options.scales.y.stacked = stacked
        config.options.scales.x.grid.display = horizontal
        valueAxis.beginAtZero = true
        if (percentAxis) {
            valueAxis.max = 100
            valueAxis.ticks = js("({ callback: function(v) { return v + '%'; } })")
        }
        if (yTitle != null) {
            config.options.scales.y.title.display = true
            config.options.scales.y.title.text = yTitle
        }
        return config
    }

    fun doughnut(labels: List<String>, values: List<Int>, colors: List<String>): dynamic {
        val config: dynamic = js("({ type: 'doughnut', data: { datasets: [{}] }, options: { plugins: { legend: { position: 'bottom' } }, cutout: '62%' } })")
        config.data.labels = labels.toTypedArray()
        config.data.datasets[0].data = values.toTypedArray()
        config.data.datasets[0].backgroundColor = colors.toTypedArray()
        config.data.datasets[0].borderWidth = 0
        config.options.maintainAspectRatio = false
        return config
    }
}
